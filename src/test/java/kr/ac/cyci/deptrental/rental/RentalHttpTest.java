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
import kr.ac.cyci.deptrental.item.ItemRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

// 실제로 서버를 띄우고, 앱처럼 주소로 요청을 보내 보는 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class RentalHttpTest {
    @LocalServerPort int port;
    @Autowired ItemRepository itemRepository;
    private final HttpClient client = HttpClient.newHttpClient();

    // 주소로 요청 보내기 (token이 있으면 로그인한 사람으로 보냄)
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

    // 응답 글자에서 "이름":값 부분의 값만 꺼냄
    private String find(String body, String regex) {
        Matcher matcher = Pattern.compile(regex).matcher(body);
        assertTrue(matcher.find(), "응답에서 값을 찾지 못했어요: " + body);
        return matcher.group(1);
    }

    @Test
    @DisplayName("주소로 신청 → 내 목록 → 취소가 된다")
    void applyListAndCancelOverHttp() throws Exception {
        // 가입 + 로그인해서 토큰 받기
        send("POST", "/api/auth/signup", null,
                "{\"password\":\"password123\",\"displayName\":\"대여 주소 테스트\",\"studentNumber\":\"20269101\",\"phoneNumber\":\"01012345678\"}");
        var login = send("POST", "/api/auth/login", null, "{\"studentNumber\":\"20269101\",\"password\":\"password123\"}");
        String token = find(login.body(), "\"accessToken\":\"([^\"]+)\"");

        Long itemId = itemRepository.search(null, null, true).get(0).getId();
        String json = "{\"itemId\":" + itemId + ",\"dueAt\":\"" + LocalDate.now().plusDays(7) + "\"}";

        // 로그인 없이 신청하면 401 + 안내 문구
        var noLogin = send("POST", "/api/rentals", null, json);
        assertEquals(401, noLogin.statusCode());
        assertTrue(noLogin.body().contains("다시 로그인해 주세요."));

        // 신청 → 201, 승인 대기
        var applied = send("POST", "/api/rentals", token, json);
        assertEquals(201, applied.statusCode());
        assertTrue(applied.body().contains("\"status\":\"PENDING\""));
        String rentalId = find(applied.body(), "\"id\":(\\d+)");

        // 내 목록 → 200, 방금 신청한 것이 보임
        var mine = send("GET", "/api/rentals/me", token, null);
        assertEquals(200, mine.statusCode());
        assertTrue(mine.body().contains("\"id\":" + rentalId + ","));

        // 취소 → 200, 취소됨
        var cancelled = send("POST", "/api/rentals/" + rentalId + "/cancel", token, null);
        assertEquals(200, cancelled.statusCode());
        assertTrue(cancelled.body().contains("\"status\":\"CANCELLED\""));
    }

    @Test
    @DisplayName("반납 예정일을 안 보내면 400 + 안내 문구")
    void missingDueDate() throws Exception {
        send("POST", "/api/auth/signup", null,
                "{\"password\":\"password123\",\"displayName\":\"대여 주소 테스트\",\"studentNumber\":\"20269102\",\"phoneNumber\":\"01012345678\"}");
        var login = send("POST", "/api/auth/login", null, "{\"studentNumber\":\"20269102\",\"password\":\"password123\"}");
        String token = find(login.body(), "\"accessToken\":\"([^\"]+)\"");

        var result = send("POST", "/api/rentals", token, "{\"itemId\":1}");
        assertEquals(400, result.statusCode());
        assertTrue(result.body().contains("반납 예정일을 선택해 주세요."));
    }
}