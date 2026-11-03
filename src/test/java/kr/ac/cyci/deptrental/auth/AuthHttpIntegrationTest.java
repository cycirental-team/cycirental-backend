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

}
