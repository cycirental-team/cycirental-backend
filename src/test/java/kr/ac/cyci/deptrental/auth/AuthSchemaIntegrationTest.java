package kr.ac.cyci.deptrental.auth;

import jakarta.persistence.EntityManager;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class AuthSchemaIntegrationTest {
    @Autowired AuthService auth;
    @Autowired JdbcTemplate jdbc;
    @Autowired TokenGenerator tokens;
    @Autowired EntityManager entities;

    @Test
    void signupUsesRemoteColumnNamesAndCannotCreateAnAdmin() {
        auth.signUp(new SignUpRequest("password123", "Schema Student", "20260001", "01012345678"));
        var result = auth.login(new LoginRequest("20260001", "password123"));
        assertFalse(result.isAdmin());
        assertEquals("01012345678", jdbc.queryForObject("SELECT phone_number FROM users WHERE user_id=?",String.class,result.userId()));
        var row = jdbc.queryForMap("SELECT user_id, login_id, password, user_name, is_admin FROM users WHERE login_id = ?", "20260001");
        assertEquals(result.userId().longValue(), ((Number) row.get("user_id")).longValue());
        assertEquals("Schema Student", row.get("user_name"));
        assertNotEquals("password123", row.get("password"));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE user_id = ? AND token_hash = ?", Integer.class, result.userId(), tokens.hash(result.accessToken())));
        auth.logout(result.accessToken());
        entities.flush();
        assertEquals(0, jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions WHERE user_id = ?", Integer.class, result.userId()));
    }

    @Test
    void adminFlagIsReadFromTheDatabaseAndRelatedRowsKeepTheirUserReference() {
        auth.signUp(new SignUpRequest("password123", "Schema Admin", "20260002", "01012345678"));
        jdbc.update("UPDATE users SET is_admin = TRUE WHERE login_id = ?", "20260002");
        entities.clear();
        var result = auth.login(new LoginRequest("20260002", "password123"));
        assertTrue(result.isAdmin());
        jdbc.update("INSERT INTO password_reset_tokens (token_hash, user_id, expires_at) VALUES (?, ?, CURRENT_TIMESTAMP)", tokens.hash("test-reset-token"), result.userId());
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM password_reset_tokens p JOIN users u ON p.user_id = u.user_id WHERE u.login_id = ?", Integer.class, "20260002"));
    }
}
