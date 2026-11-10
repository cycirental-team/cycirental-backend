package kr.ac.cyci.deptrental.auth;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuthHttpIntegrationTest {
    @LocalServerPort int port;
    private final HttpClient client = HttpClient.newHttpClient();

    private HttpResponse<String> post(String path, String json) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    @Test
    void invalidSignupReturnsSpecific400DetailsWithoutSaving() throws Exception {
        var result = post("/api/auth/signup", "{\"loginId\":\"a\",\"password\":\"short\",\"phoneNumber\":\"01012345678\",\"displayName\":\"Tester\",\"studentNumber\":\"invalid\"}");
        assertEquals(400, result.statusCode());
        assertTrue(result.body().contains("학번은"));
        assertFalse(result.body().contains("short"));
    }

    @Test
    void signupLoginAndDuplicateErrorsUseTheRemoteColumnMapping() throws Exception {
        var payload = "{\"password\":\"password123\",\"phoneNumber\":\"01012345678\",\"displayName\":\"Http Tester\",\"studentNumber\":\"20260003\"}";
        assertEquals(201, post("/api/auth/signup", payload).statusCode());
        var duplicate = post("/api/auth/signup", payload);
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("이미 등록된 학번"));
        var login = post("/api/auth/login", "{\"studentNumber\":\"20260003\",\"password\":\"password123\"}");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"isAdmin\":false"));
        assertTrue(login.body().contains("\"accessToken\":"));
        var wrong = post("/api/auth/login", "{\"studentNumber\":\"20260003\",\"password\":\"wrongpass\"}");
        assertEquals(401, wrong.statusCode());
        assertTrue(wrong.body().contains("학번 또는 비밀번호"));
    }

    @Test
    void leadingZeroStudentNumberIsPreservedAndDuplicateIsRejected() throws Exception {
        var payload = "{\"password\":\"password123\",\"phoneNumber\":\"01012345678\",\"displayName\":\"Student Zero\",\"studentNumber\":\"00012345\"}";
        assertEquals(201, post("/api/auth/signup", payload).statusCode());
        var login = post("/api/auth/login", "{\"studentNumber\":\"00012345\",\"password\":\"password123\"}");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"studentNumber\":\"00012345\""));
        var duplicate = post("/api/auth/signup", payload.replace("leading_zero_student", "different_student"));
        assertEquals(409, duplicate.statusCode());
        assertTrue(duplicate.body().contains("이미 등록된 학번"));
    }
    @Test
    void signupRejectsMissingAndInvalidPhoneNumbers() throws Exception {
        var payload = "{\"password\":\"password123\",\"displayName\":\"Phone Tester\",\"studentNumber\":\"20260088\"}";
        assertEquals(400, post("/api/auth/signup",payload).statusCode());
        var invalid = payload.replace("}",",\"phoneNumber\":\"12345\"}");
        var response = post("/api/auth/signup",invalid);
        assertEquals(400,response.statusCode());
        assertTrue(response.body().contains("휴대전화번호"));
    }

    @org.springframework.beans.factory.annotation.Autowired
    org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Test
    void legacyFrontendPayloadIsRejectedAndFourFieldSignupMapsToExistingColumns() throws Exception {
        long before = jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class);
        var legacy = post("/api/auth/signup", """
            {"loginId":"old_student","email":"unused@example.invalid","displayName":"기존 화면","password":"password123"}
            """);
        assertEquals(400, legacy.statusCode());
        assertTrue(legacy.body().contains("학번을 입력해 주세요."));
        assertTrue(legacy.body().contains("전화번호를 입력해 주세요."));
        assertEquals(before, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));

        var signup = post("/api/auth/signup", """
            {"displayName":"연동 학생","studentNumber":"000202611004","password":"password123","phoneNumber":"01000000000"}
            """);
        assertEquals(201, signup.statusCode());
        assertEquals(before + 1, jdbc.queryForObject("SELECT COUNT(*) FROM users", Long.class));
        var row = jdbc.queryForMap("SELECT user_name, login_id, phone_number, password, is_admin FROM users WHERE student_number = ?", "000202611004");
        assertEquals("연동 학생", row.get("user_name"));
        assertEquals("000202611004", row.get("login_id"));
        assertEquals("01000000000", row.get("phone_number"));
        assertNotEquals("password123", row.get("password"));
        assertEquals(false, row.get("is_admin"));

        var oldLogin = post("/api/auth/login", """
            {"loginId":"000202611004","password":"password123"}
            """);
        assertEquals(400, oldLogin.statusCode());
        assertTrue(oldLogin.body().contains("학번을 입력해 주세요."));
        var login = post("/api/auth/login", """
            {"studentNumber":"000202611004","password":"password123"}
            """);
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"displayName\":\"연동 학생\""));
        assertTrue(login.body().contains("\"studentNumber\":\"000202611004\""));
        assertTrue(login.body().contains("\"accessToken\":"));
    }

    @Test
    void reservedAdminUsesDatabasePasswordAndCannotBeCreatedBySignup() throws Exception {
        var sql = new org.springframework.core.io.ClassPathResource("db/manual/create_reserved_admin_001.sql")
            .getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        var insert = sql.substring(sql.indexOf("INSERT INTO users"));
        assertEquals(1, jdbc.update(insert));
        assertEquals(0, jdbc.update(insert));
        var login = post("/api/auth/login", "{\"studentNumber\":\"001\",\"password\":\"admin\"}");
        assertEquals(200, login.statusCode());
        assertTrue(login.body().contains("\"isAdmin\":true"));
        assertTrue(login.body().contains("\"studentNumber\":\"001\""));
        assertEquals(401, post("/api/auth/login", "{\"studentNumber\":\"001\",\"password\":\"wrong\"}").statusCode());
        assertEquals(400, post("/api/auth/login", "{\"studentNumber\":\"002\",\"password\":\"admin\"}").statusCode());
        assertEquals(400, post("/api/auth/signup", "{\"studentNumber\":\"001\",\"password\":\"password123\",\"displayName\":\"Fake Admin\",\"phoneNumber\":\"01012345678\"}").statusCode());
    }


    private HttpResponse<String> postAs(String path, String json, String token) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create("http://127.0.0.1:" + port + path))
                .header("Content-Type", "application/json")
                .header("Authorization", "Bearer " + token)
                .POST(HttpRequest.BodyPublishers.ofString(json)).build(), HttpResponse.BodyHandlers.ofString());
    }

    private String responseField(HttpResponse<String> response, String field) {
        var matcher = java.util.regex.Pattern.compile("\"" + field + "\"\\s*:\\s*\"([^\"]+)\"").matcher(response.body());
        assertTrue(matcher.find(), "Missing response field: " + field);
        return matcher.group(1);
    }

    @Test
    void rejectingAnotherResetAfterPasswordChangePreservesTheNewPassword() throws Exception {
        assertEquals(201, post("/api/auth/signup", """
            {"displayName":"Recovery Student","studentNumber":"20262001","password":"originalPassword123","phoneNumber":"01000000001"}
            """).statusCode());
        assertEquals(201, post("/api/auth/signup", """
            {"displayName":"Recovery Admin","studentNumber":"20262002","password":"adminPassword123","phoneNumber":"01000000002"}
            """).statusCode());
        jdbc.update("UPDATE users SET is_admin=TRUE WHERE student_number=?", "20262002");
        var admin = post("/api/auth/login", """
            {"studentNumber":"20262002","password":"adminPassword123"}
            """);
        assertEquals(200, admin.statusCode());
        String adminToken = responseField(admin, "accessToken");
        String resetRequest = """
            {"displayName":"Recovery Student","studentNumber":"20262001"}
            """;
        String pendingSql = "SELECT id FROM password_reset_requests WHERE user_id=(SELECT user_id FROM users WHERE student_number='20262001') AND status='PENDING'";
        assertEquals(202, post("/api/auth/password-reset/requests", resetRequest).statusCode());
        Long firstId = jdbc.queryForObject(pendingSql, Long.class);
        var approved = postAs("/api/admin/password-reset-requests/" + firstId + "/approve", "{\"identityConfirmed\":true}", adminToken);
        assertEquals(200, approved.statusCode());
        String temporary = responseField(approved, "temporaryPassword");
        var limited = post("/api/auth/login", "{\"studentNumber\":\"20262001\",\"password\":\"" + temporary + "\"}");
        assertEquals(200, limited.statusCode());
        assertTrue(limited.body().contains("\"mustChangePassword\":true"));
        var changed = postAs("/api/auth/password/change", "{\"currentPassword\":\"" + temporary + "\",\"newPassword\":\"changedPassword456\"}", responseField(limited, "accessToken"));
        assertEquals(200, changed.statusCode());
        String currentLogin = "{\"studentNumber\":\"20262001\",\"password\":\"changedPassword456\"}";
        assertEquals(200, post("/api/auth/login", currentLogin).statusCode());
        String savedHash = jdbc.queryForObject("SELECT password FROM users WHERE student_number=?", String.class, "20262001");
        assertEquals(202, post("/api/auth/password-reset/requests", resetRequest).statusCode());
        Long secondId = jdbc.queryForObject(pendingSql, Long.class);
        assertNotEquals(firstId, secondId);
        assertEquals(200, postAs("/api/admin/password-reset-requests/" + secondId + "/reject", "{}", adminToken).statusCode());
        assertEquals("REJECTED", jdbc.queryForObject("SELECT status FROM password_reset_requests WHERE id=?", String.class, secondId));
        assertEquals(savedHash, jdbc.queryForObject("SELECT password FROM users WHERE student_number=?", String.class, "20262001"));
        var loginAfterRejection = post("/api/auth/login", currentLogin);
        assertEquals(200, loginAfterRejection.statusCode());
        assertTrue(loginAfterRejection.body().contains("\"mustChangePassword\":false"));
        assertEquals(401, post("/api/auth/login", "{\"studentNumber\":\"20262001\",\"password\":\"originalPassword123\"}").statusCode());
        assertEquals(401, post("/api/auth/login", "{\"studentNumber\":\"20262001\",\"password\":\"" + temporary + "\"}").statusCode());
    }

}
