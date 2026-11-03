package kr.ac.cyci.deptrental.auth;

import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;

class UserSchemaUpgradeTest {
    @Test
    void forwardMigrationKeepsExistingUsersRentalsAndSessions() {
        var url = "jdbc:h2:mem:upgrade_review;MODE=MariaDB;DB_CLOSE_DELAY=-1";
        Flyway.configure().dataSource(url, "sa", "").target("3").load().migrate();
        var jdbc = new JdbcTemplate(new DriverManagerDataSource(url, "sa", ""));
        jdbc.update("INSERT INTO users (student_id, password_hash, display_name, email) VALUES (?, ?, ?, ?)",
                "existing_user", "existing-password-hash", "Existing User", "existing@example.invalid");
        var userId = jdbc.queryForObject("SELECT id FROM users WHERE student_id = ?", Long.class, "existing_user");
        var itemId = jdbc.queryForObject("SELECT MIN(id) FROM items", Long.class);
        jdbc.update("INSERT INTO rentals (user_id, item_id, rented_at, due_at, status) VALUES (?, ?, CURRENT_DATE, CURRENT_DATE, ?)", userId, itemId, "RENTED");
        jdbc.update("INSERT INTO auth_sessions (token_hash, user_id, expires_at) VALUES (?, ?, CURRENT_TIMESTAMP)", "existing-session-hash", userId);
        Flyway.configure().dataSource(url, "sa", "").load().migrate();
        assertEquals(userId, jdbc.queryForObject("SELECT user_id FROM users WHERE login_id = ?", Long.class, "existing_user"));
        assertEquals("existing-password-hash", jdbc.queryForObject("SELECT password FROM users WHERE user_id = ?", String.class, userId));
        assertNull(jdbc.queryForObject("SELECT phone_number FROM users WHERE user_id = ?", String.class, userId));
        assertNull(jdbc.queryForObject("SELECT student_number FROM users WHERE user_id = ?", String.class, userId));
        assertEquals("existing@example.invalid", jdbc.queryForObject("SELECT email FROM users WHERE user_id = ?", String.class, userId));
        assertEquals("Existing User", jdbc.queryForObject("SELECT user_name FROM users WHERE user_id = ?", String.class, userId));
        assertEquals(false, jdbc.queryForObject("SELECT is_admin FROM users WHERE user_id = ?", Boolean.class, userId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM rentals r JOIN users u ON r.user_id = u.user_id WHERE u.user_id = ?", Integer.class, userId));
        assertEquals(1, jdbc.queryForObject("SELECT COUNT(*) FROM auth_sessions s JOIN users u ON s.user_id = u.user_id WHERE u.user_id = ?", Integer.class, userId));
    }
}
