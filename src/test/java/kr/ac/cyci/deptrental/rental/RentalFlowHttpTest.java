package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

// 앱이 쓰는 순서 그대로, 실제 주소로 처음부터 끝까지 보내 보는 테스트
//   학생 신청 → 관리자 승인 → 학생 반납 요청 → 관리자 반납 완료
// 기존 물품 5개의 수량은 다른 테스트가 확인하므로, 이 테스트 전용 물품을 만들고 끝나면 지움
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RentalFlowHttpTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) {
            builder.header("Authorization", "Bearer " + token);
        }
        builder.method(method, json == null
                ? HttpRequest.BodyPublishers.noBody()
                : HttpRequest.BodyPublishers.ofString(json));
        return client.send(builder.build(), HttpResponse.BodyHandlers.ofString());
    }

    private String find(String body, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(body);
        assertTrue(matcher.find(), "응답에서 값을 찾지 못했어요: " + body);
        return matcher.group(1);
    }

    // 가입 + 로그인해서 토큰 받기 (admin이 true면 관리자로 바꾼 뒤 로그인)
    private String login(String studentNumber, boolean admin) throws Exception {
        send("POST", "/api/auth/signup", null,
                "{\"password\":\"password123\",\"displayName\":\"흐름 테스트\",\"studentNumber\":\"" + studentNumber + "\",\"phoneNumber\":\"01012345678\"}");
        if (admin) {
            jdbc.update("UPDATE users SET is_admin = TRUE WHERE student_number = ?", studentNumber);
        }
        var login = send("POST", "/api/auth/login", null,
                "{\"studentNumber\":\"" + studentNumber + "\",\"password\":\"password123\"}");
        return find(login.body(), "\"accessToken\":\"([^\"]+)\"");
    }

    private int availableQuantity(Long itemId) {
        return jdbc.queryForObject("SELECT available_quantity FROM items WHERE id = ?", Integer.class, itemId);
    }

    @Test
    @DisplayName("신청 → 승인 → 반납 요청 → 반납 완료가 주소로 이어지고, 재고가 2 → 1 → 2로 바뀐다")
    void fullRentalFlow() throws Exception {
        String admin = login("20269901", true);
        String student = login("20269902", false);
        jdbc.update("INSERT INTO items (name, category, description, total_quantity, available_quantity) VALUES (?, ?, ?, ?, ?)",
                "대여 흐름 테스트 물품", "테스트", "흐름 테스트 전용", 2, 2);
        Long itemId = jdbc.queryForObject("SELECT MAX(id) FROM items WHERE name = ?", Long.class, "대여 흐름 테스트 물품");
        try {
            // 1. 학생 신청 → 승인 대기
            var applied = send("POST", "/api/rentals", student,
                    "{\"itemId\":" + itemId + ",\"dueAt\":\"" + LocalDate.now().plusDays(7) + "\"}");
            assertEquals(201, applied.statusCode());
            String rentalId = find(applied.body(), "\"id\":(\\d+)");

            // 2. 관리자 승인 → 대여 중, 재고 2 → 1
            var approved = send("POST", "/api/admin/rentals/" + rentalId + "/approve", admin, null);
            assertEquals(200, approved.statusCode());
            assertTrue(approved.body().contains("\"status\":\"RENTED\""));
            assertEquals(1, availableQuantity(itemId));

            // 3. 반납 요청 전에 관리자가 반납 완료를 누르면 409 + 안내 문구
            var tooEarly = send("POST", "/api/admin/rentals/" + rentalId + "/return", admin, null);
            assertEquals(409, tooEarly.statusCode());
            assertTrue(tooEarly.body().contains("반납 요청된 대여만"));

            // 4. 학생 반납 요청 → 반납 요청 (재고는 아직 1)
            var requested = send("POST", "/api/rentals/" + rentalId + "/return-request", student, null);
            assertEquals(200, requested.statusCode());
            assertTrue(requested.body().contains("\"status\":\"RETURN_REQUESTED\""));
            assertEquals(1, availableQuantity(itemId));

            // 5. 관리자 반납 완료 → 반납 완료, 반납일 기록, 재고 1 → 2
            var completed = send("POST", "/api/admin/rentals/" + rentalId + "/return", admin, null);
            assertEquals(200, completed.statusCode());
            assertTrue(completed.body().contains("\"status\":\"RETURNED\""));
            assertTrue(completed.body().contains("\"returnedAt\":\"" + LocalDate.now() + "\""));
            assertEquals(2, availableQuantity(itemId));

            // 6. 학생 내 대여 목록에도 반납 완료와 반납일이 보임 (앱은 반납 완료에 반납일이 없으면 오류)
            var mine = send("GET", "/api/rentals/me", student, null);
            assertEquals(200, mine.statusCode());
            assertTrue(mine.body().contains("\"status\":\"RETURNED\""));
            assertTrue(mine.body().contains("\"returnedAt\":\"" + LocalDate.now() + "\""));
        } finally {
            jdbc.update("DELETE FROM rentals WHERE item_id = ?", itemId);
            jdbc.update("DELETE FROM items WHERE id = ?", itemId);
        }
    }
}