package kr.ac.cyci.deptrental.auth;

import jakarta.persistence.EntityManager;
import java.time.Instant;
import kr.ac.cyci.deptrental.auth.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest @Transactional
class AccountRecoveryIntegrationTest {
    @Autowired AuthService auth;
    @Autowired UserRepository users;
    @Autowired AuthSessionRepository sessions;
    @Autowired AccountRecoveryRepository requests;
    @Autowired JdbcTemplate jdbc;
    @Autowired EntityManager em;
    @Autowired TokenGenerator tokens;

    private AuthResponse account(String number, boolean admin) {
        auth.signUp(new SignUpRequest("password123", "학생 이름", number, "01012345678"));
        em.flush();
        if(admin) { jdbc.update("UPDATE users SET is_admin=TRUE WHERE student_number=?",number); em.clear(); }
        return auth.login(new LoginRequest(number,"password123"));
    }
    private void assertStatus(int status, Runnable action) {
        assertEquals(status,assertThrows(ResponseStatusException.class,action::run).getStatusCode().value());
    }
    @Test void pendingRequestsAreDeduplicatedAndUnknownAccountsAreNotExposed() {
        account("20261001",false);
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261001"));
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261001"));
        auth.requestPasswordReset(new PasswordResetRequest("다른 이름","20261001"));
        auth.requestPasswordReset(new PasswordResetRequest("없는 학생","20269999"));
        assertEquals(1,requests.count());
    }
    @Test void adminAuthorizationRejectsMissingStudentAndExpiredTokens() {
        var student=account("20261002",false);
        assertStatus(401,()->auth.listRecovery(null));
        assertStatus(403,()->auth.listRecovery(student.accessToken()));
        var admin=account("20261003",true);
        em.flush();
        jdbc.update("UPDATE auth_sessions SET expires_at=? WHERE token_hash=?",java.sql.Timestamp.from(Instant.now().minusSeconds(1)),tokens.hash(admin.accessToken()));
        em.clear();
        assertStatus(401,()->auth.listRecovery(admin.accessToken()));
    }
    @Test void resetRequiresIdentityConfirmationThenForcesChangeAndRevokesOldSessions() {
        var student=account("20261004",false);
        var admin=account("20261005",true);
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261004"));
        Long id=auth.listRecovery(admin.accessToken()).getFirst().id();
        assertStatus(403,()->auth.processRecovery(student.accessToken(),id,true,true));
        assertStatus(400,()->auth.processRecovery(admin.accessToken(),id,true,false));
        var temporary=auth.processRecovery(admin.accessToken(),id,true,true);
        assertEquals(10,temporary.temporaryPassword().length());
        assertTrue(temporary.temporaryPassword().matches("[A-HJKMNP-Z2-9]+"));
        assertEquals(43,admin.accessToken().length());
        assertNotEquals(temporary.temporaryPassword(),users.findByStudentNumber("20261004").orElseThrow().getPasswordHash());
        assertTrue(temporary.expiresAt().isAfter(Instant.now()));
        assertStatus(409,()->auth.processRecovery(admin.accessToken(),id,true,true));
        assertStatus(401,()->auth.login(new LoginRequest("20261004","password123")));
        assertStatus(401,()->auth.changePassword(student.accessToken(),new ChangePasswordRequest("password123","newPassword123")));
        var limited=auth.login(new LoginRequest("20261004",temporary.temporaryPassword()));
        assertTrue(limited.mustChangePassword());
        assertStatus(400,()->auth.changePassword(limited.accessToken(),new ChangePasswordRequest("incorrect","newPassword123")));
        var changed=auth.changePassword(limited.accessToken(),new ChangePasswordRequest(temporary.temporaryPassword(),"newPassword123"));
        assertFalse(changed.mustChangePassword());
        assertStatus(401,()->auth.changePassword(limited.accessToken(),new ChangePasswordRequest("newPassword123","newPassword456")));
        assertStatus(401,()->auth.login(new LoginRequest("20261004",temporary.temporaryPassword())));
        assertFalse(auth.login(new LoginRequest("20261004","newPassword123")).mustChangePassword());
    }
    @Test void temporaryAdminCannotUseAdminEndpointsUntilPasswordChanges() {
        var target=account("20261006",true);
        var admin=account("20261007",true);
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261006"));
        var temporary=auth.processRecovery(admin.accessToken(),auth.listRecovery(admin.accessToken()).getFirst().id(),true,true);
        var limited=auth.login(new LoginRequest("20261006",temporary.temporaryPassword()));
        assertTrue(limited.isAdmin());
        assertStatus(403,()->auth.listRecovery(limited.accessToken()));
        var changed=auth.changePassword(limited.accessToken(),new ChangePasswordRequest(temporary.temporaryPassword(),"adminPassword123"));
        assertNotNull(auth.listRecovery(changed.accessToken()));
    }
    @Test void expiredTemporaryPasswordCannotLogin() {
        account("20261008",false);
        users.findByStudentNumber("20261008").orElseThrow().issueTemporaryPassword(
            new PasswordHasher().hash("temporary123"),Instant.now().minusSeconds(1));
        assertStatus(401,()->auth.login(new LoginRequest("20261008","temporary123")));
    }
    @Test void rejectedRequestLeavesPasswordUnchangedAndAllowsNewRequest() {
        account("20261009",false);
        var admin=account("20261010",true);
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261009"));
        Long id=auth.listRecovery(admin.accessToken()).getFirst().id();
        assertNull(auth.processRecovery(admin.accessToken(),id,false,false));
        assertFalse(auth.login(new LoginRequest("20261009","password123")).mustChangePassword());
        auth.requestPasswordReset(new PasswordResetRequest("학생 이름","20261009"));
        assertEquals(1,auth.listRecovery(admin.accessToken()).size());
    }
}
