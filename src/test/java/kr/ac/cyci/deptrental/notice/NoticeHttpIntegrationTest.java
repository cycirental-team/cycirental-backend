package kr.ac.cyci.deptrental.notice;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.jdbc.core.JdbcTemplate;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class NoticeHttpIntegrationTest {
    @LocalServerPort int port;
    @Autowired JdbcTemplate jdbc;
    private final HttpClient client = HttpClient.newHttpClient();

    @Test
    void publicCanReadAndOnlyAdminCanManageNotices() throws Exception {
        String admin = login("20269901", true);
        String student = login("20269902", false);

        var forbidden = send("POST", "/api/admin/notices", student,
                "{\"title\":\"학생 작성\",\"content\":\"내용\",\"important\":false}");
        assertEquals(403, forbidden.statusCode());

        var regular = send("POST", "/api/admin/notices", admin,
                "{\"title\":\"일반 공지\",\"content\":\"일반 내용\",\"important\":false}");
        var important = send("POST", "/api/admin/notices", admin,
                "{\"title\":\"중요 공지\",\"content\":\"중요 내용\",\"important\":true}");
        assertEquals(201, regular.statusCode());
        assertEquals(201, important.statusCode());

        var list = send("GET", "/api/notices", null, null);
        assertEquals(200, list.statusCode());
        assertTrue(list.body().indexOf("중요 공지") < list.body().indexOf("일반 공지"));

        long id = idFrom(important.body());
        var updated = send("PUT", "/api/admin/notices/" + id, admin,
                "{\"title\":\"수정된 공지\",\"content\":\"수정 내용\",\"important\":false}");
        assertEquals(200, updated.statusCode());
        assertTrue(updated.body().contains("수정된 공지"));

        var detail = send("GET", "/api/notices/" + id, null, null);
        assertEquals(200, detail.statusCode());
        assertTrue(detail.body().contains("수정 내용"));

        assertEquals(204, send("DELETE", "/api/admin/notices/" + id, admin, null).statusCode());
        assertEquals(404, send("GET", "/api/notices/" + id, null, null).statusCode());
    }

    private String login(String studentNumber, boolean admin) throws Exception {
        send("POST", "/api/auth/signup", null,
                "{\"password\":\"password123\",\"displayName\":\"공지 테스트\",\"studentNumber\":\"" + studentNumber + "\",\"phoneNumber\":\"01012345678\"}");
        if (admin) {
            jdbc.update("UPDATE users SET is_admin = TRUE WHERE student_number = ?", studentNumber);
        }
        var response = send("POST", "/api/auth/login", null,
                "{\"studentNumber\":\"" + studentNumber + "\",\"password\":\"password123\"}");
        Matcher matcher = Pattern.compile("\\\"accessToken\\\":\\\"([^\\\"]+)\\\"").matcher(response.body());
        assertTrue(matcher.find(), response.body());
        return matcher.group(1);
    }

    private long idFrom(String body) {
        Matcher matcher = Pattern.compile("\\\"id\\\":(\\d+)").matcher(body);
        assertTrue(matcher.find(), body);
        return Long.parseLong(matcher.group(1));
    }

    private HttpResponse<String> send(String method, String path, String token, String json) throws Exception {
        var builder = HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json");
        if (token != null) builder.header("Authorization", "Bearer " + token);
        return client.send(builder.method(method, json == null
                        ? HttpRequest.BodyPublishers.noBody()
                        : HttpRequest.BodyPublishers.ofString(json))
                .build(), HttpResponse.BodyHandlers.ofString());
    }
}
