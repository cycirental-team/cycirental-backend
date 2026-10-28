package kr.ac.cyci.deptrental.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import kr.ac.cyci.deptrental.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional
public class AuthService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final PasswordResetTokenRepository resetTokens;
    private final PasswordHasher passwordHasher;
    private final TokenGenerator tokenGenerator;
    private final PasswordResetNotifier resetNotifier;

    public AuthService(UserRepository users, AuthSessionRepository sessions, PasswordResetTokenRepository resetTokens,
                       PasswordHasher passwordHasher, TokenGenerator tokenGenerator, PasswordResetNotifier resetNotifier) {
        this.users = users; this.sessions = sessions; this.resetTokens = resetTokens;
        this.passwordHasher = passwordHasher; this.tokenGenerator = tokenGenerator; this.resetNotifier = resetNotifier;
    }

    public void signUp(SignUpRequest request) {
        String loginId = request.loginId().trim();
        String email = request.email().trim().toLowerCase();
        if (users.existsByLoginId(loginId)) throw conflict("이미 사용 중인 아이디입니다.");
        if (users.existsByEmail(email)) throw conflict("이미 사용 중인 이메일입니다.");
        users.save(new User(loginId, passwordHasher.hash(request.password()), request.displayName().trim(), email));
    }

    public AuthResponse login(LoginRequest request) {
        User user = users.findByLoginId(request.loginId().trim())
                .orElseThrow(() -> unauthorized("아이디 또는 비밀번호가 올바르지 않습니다."));
        if (!passwordHasher.matches(request.password(), user.getPasswordHash())) throw unauthorized("아이디 또는 비밀번호가 올바르지 않습니다.");
        String token = tokenGenerator.create();
        sessions.save(new AuthSession(tokenGenerator.hash(token), user, Instant.now().plus(7, ChronoUnit.DAYS)));
        return new AuthResponse(user.getId(), user.getLoginId(), user.getDisplayName(), token, "Bearer");
    }

    @Transactional(readOnly = true)
    public String findId(FindIdRequest request) {
        return users.findByEmailAndDisplayName(request.email().trim().toLowerCase(), request.displayName().trim())
                .map(User::getLoginId).orElseThrow(() -> notFound("일치하는 회원 정보를 찾을 수 없습니다."));
    }

    public void requestPasswordReset(PasswordResetRequest request) {
        users.findByLoginId(request.loginId().trim())
                .filter(user -> user.getEmail().equalsIgnoreCase(request.email().trim()))
                .ifPresent(user -> {
                    String token = tokenGenerator.create();
                    resetTokens.save(new PasswordResetToken(tokenGenerator.hash(token), user, Instant.now().plus(30, ChronoUnit.MINUTES)));
                    resetNotifier.send(user.getEmail(), token);
                });
    }

    public void resetPassword(PasswordResetConfirmRequest request) {
        PasswordResetToken resetToken = resetTokens.findByTokenHash(tokenGenerator.hash(request.token()))
                .filter(token -> token.isUsable(Instant.now()))
                .orElseThrow(() -> badRequest("유효하지 않거나 만료된 비밀번호 재설정 요청입니다."));
        resetToken.getUser().changePassword(passwordHasher.hash(request.newPassword()));
        resetToken.markUsed(Instant.now());
    }

    public void logout(String accessToken) {
        if (accessToken != null && !accessToken.isBlank()) {
            sessions.deleteByTokenHash(tokenGenerator.hash(accessToken));
        }
    }

    private ResponseStatusException conflict(String message) { return new ResponseStatusException(HttpStatus.CONFLICT, message); }
    private ResponseStatusException unauthorized(String message) { return new ResponseStatusException(HttpStatus.UNAUTHORIZED, message); }
    private ResponseStatusException notFound(String message) { return new ResponseStatusException(HttpStatus.NOT_FOUND, message); }
    private ResponseStatusException badRequest(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
