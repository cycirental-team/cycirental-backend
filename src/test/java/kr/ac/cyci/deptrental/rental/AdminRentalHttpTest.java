package kr.ac.cyci.deptrental.rental;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

// 실제로 서버를 띄우고, 관리자 목록 주소로 요청을 보내 보는 테스트
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AdminRentalHttpTest {
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

    // 가입 + 로그인해서 토큰 받기 (admin이 true면 관리자로 바꾼 뒤 로그인)
    private String login(String studentNumber, boolean admin) throws Exception {
        send("POST", "/api/auth/signup", null,
                "{\"password\":\"password123\",\"displayName\":\"관리자 주소 테스트\",\"studentNumber\":\"" + studentNumber + "\",\"phoneNumber\":\"01012345678\"}");
        if (admin) {
            jdbc.update("UPDATE users SET is_admin = TRUE WHERE student_number = ?", studentNumber);
        }
        var login = send("POST", "/api/auth/login", null,
                "{\"studentNumber\":\"" + studentNumber + "\",\"password\":\"password123\"}");
        Matcher matcher = Pattern.compile("\"accessToken\":\"([^\"]+)\"").matcher(login.body());
        assertTrue(matcher.find(), "로그인 응답에서 토큰을 찾지 못했어요: " + login.body());
        return matcher.group(1);
    }

    @Test
    @DisplayName("관리자는 목록을 받고, 학생은 403 + 안내 문구")
    void adminOnly() throws Exception {
        String admin = login("20269601", true);
        String student = login("20269602", false);

        var ok = send("GET", "/api/admin/rentals", admin, null);
        assertEquals(200, ok.statusCode());
        assertTrue(ok.body().startsWith("["));

        var forbidden = send("GET", "/api/admin/rentals", student, null);
        assertEquals(403, forbidden.statusCode());
        assertTrue(forbidden.body().contains("관리자 권한"));
    }

    @Test
    @DisplayName("status 값이 틀리면 400 + 안내 문구")
    void wrongStatus() throws Exception {
        String admin = login("20269603", true);

        var result = send("GET", "/api/admin/rentals?status=WRONG", admin, null);

        assertEquals(400, result.statusCode());
        assertTrue(result.body().contains("요청 값을 확인해 주세요."));
    }
    @Test
    @DisplayName("거절 사유가 비었거나 300자를 넘으면 400 + 안내 문구")
    void rejectReasonRule() throws Exception {
        String admin = login("20269604", true);

        var blank = send("POST", "/api/admin/rentals/1/reject", admin, "{\"reason\":\"   \"}");
        assertEquals(400, blank.statusCode());
        assertTrue(blank.body().contains("거절 사유를 입력해 주세요."));

        var tooLong = send("POST", "/api/admin/rentals/1/reject", admin, "{\"reason\":\"" + "가".repeat(301) + "\"}");
        assertEquals(400, tooLong.statusCode());
        assertTrue(tooLong.body().contains("300자 이내"));
    }

}