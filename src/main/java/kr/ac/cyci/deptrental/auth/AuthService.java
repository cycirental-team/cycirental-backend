package kr.ac.cyci.deptrental.auth;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import kr.ac.cyci.deptrental.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service @Transactional
public class AuthService {
    private final UserRepository users;
    private final AuthSessionRepository sessions;
    private final AccountRecoveryRepository recovery;
    private final PasswordHasher passwords;
    private final TokenGenerator tokens;
    public AuthService(UserRepository users,AuthSessionRepository sessions,AccountRecoveryRepository recovery,
                       PasswordHasher passwords,TokenGenerator tokens) {
        this.users=users; this.sessions=sessions; this.recovery=recovery; this.passwords=passwords; this.tokens=tokens;
    }
    public void signUp(SignUpRequest request) {
        String number=request.studentNumber();
        if(users.existsByStudentNumber(number) || users.existsByLoginId(number))
            throw error(HttpStatus.CONFLICT,"이미 등록된 학번입니다. 기존 계정은 관리자에게 확인해 주세요.");
        users.save(new User(number,passwords.hash(request.password()),request.displayName().trim(),number,request.phoneNumber()));
    }
    public AuthResponse login(LoginRequest request) {
        User user=users.findByStudentNumber(request.studentNumber()).orElseThrow(this::invalidLogin);
        if("001".equals(request.studentNumber()) && !user.isAdmin()) throw invalidLogin();
        if(!passwords.matches(request.password(),user.getPasswordHash())) throw invalidLogin();
        if(user.temporaryPasswordExpired(Instant.now())) throw error(HttpStatus.UNAUTHORIZED,"임시 비밀번호가 만료되었습니다. 초기화를 다시 요청해 주세요.");
        return issueSession(user);
    }
    private ResponseStatusException invalidLogin(){return error(HttpStatus.UNAUTHORIZED,"학번 또는 비밀번호가 올바르지 않습니다.");}
    private AuthResponse issueSession(User user) {
        String token=tokens.create();
        sessions.save(new AuthSession(tokens.hash(token),user,Instant.now().plus(user.mustChangePassword()?1:7,ChronoUnit.DAYS)));
        return new AuthResponse(user.getId(),user.getLoginId(),user.getDisplayName(),token,"Bearer",user.isAdmin(),user.getStudentNumber(),user.mustChangePassword());
    }
    public void requestPasswordReset(PasswordResetRequest request) {
        // Lock the account so simultaneous requests cannot create duplicate pending rows.
        users.lockByStudentNumber(request.studentNumber())
            .filter(user -> request.displayName().trim().equals(user.getDisplayName()))
            .ifPresent(user -> {
                if(!recovery.existsByUserAndStatus(user,"PENDING")) recovery.save(new AccountRecoveryRequest(user));
            });
    }
    private User authenticate(String token) {
        if(token==null || token.isBlank()) throw error(HttpStatus.UNAUTHORIZED,"다시 로그인해 주세요.");
        AuthSession session=sessions.findByTokenHash(tokens.hash(token))
            .filter(s -> !s.isExpired(Instant.now()))
            .orElseThrow(() -> error(HttpStatus.UNAUTHORIZED,"로그인이 만료되었습니다. 다시 로그인해 주세요."));
        User user=session.getUser();
        if(user.temporaryPasswordExpired(Instant.now())) throw error(HttpStatus.UNAUTHORIZED,"임시 비밀번호가 만료되었습니다.");
        return user;
    }
    private User requireAdmin(String token) {
        User user=authenticate(token);
        if(!user.isAdmin() || user.mustChangePassword()) throw error(HttpStatus.FORBIDDEN,"관리자 권한과 비밀번호 변경 완료가 필요합니다.");
        return user;
    }
    public record RecoveryView(Long id,String displayName,String studentNumber,Instant requestedAt,String status) { }
    public List<RecoveryView> listRecovery(String token) {
        requireAdmin(token);
        return recovery.findTop100ByStatusOrderByRequestedAtAsc("PENDING").stream()
            .map(r -> new RecoveryView(r.getId(),r.getUser().getDisplayName(),r.getUser().getStudentNumber(),r.getRequestedAt(),r.getStatus())).toList();
    }
    public record TemporaryPassword(String temporaryPassword,Instant expiresAt) { }
    public TemporaryPassword processRecovery(String token,Long id,boolean approve,boolean identityConfirmed) {
        User admin=requireAdmin(token);
        AccountRecoveryRequest request=recovery.lockById(id).orElseThrow(() -> error(HttpStatus.NOT_FOUND,"요청을 찾을 수 없습니다."));
        if(!request.getStatus().equals("PENDING")) throw error(HttpStatus.CONFLICT,"이미 처리된 요청입니다.");
        if(approve && !identityConfirmed) throw error(HttpStatus.BAD_REQUEST,"본인 확인을 먼저 완료해 주세요.");
        TemporaryPassword result=null;
        if(approve) {
            User user=users.lockByStudentNumber(request.getUser().getStudentNumber()).orElseThrow();
            String password=tokens.createTemporaryPassword();
            Instant expiry=Instant.now().plus(1,ChronoUnit.DAYS);
            user.issueTemporaryPassword(passwords.hash(password),expiry);
            sessions.deleteByUser(user);
            result=new TemporaryPassword(password,expiry);
        }
        request.finish(admin,approve);
        return result;
    }
    public AuthResponse changePassword(String token,ChangePasswordRequest request) {
        User authenticated=authenticate(token);
        User user=users.lockByStudentNumber(authenticated.getStudentNumber()).orElseThrow();
        if(!passwords.matches(request.currentPassword(),user.getPasswordHash())) throw error(HttpStatus.BAD_REQUEST,"현재 비밀번호를 확인해 주세요.");
        if(request.newPassword().equals(request.currentPassword())) throw error(HttpStatus.BAD_REQUEST,"다른 새 비밀번호를 입력해 주세요.");
        user.changePassword(passwords.hash(request.newPassword()));
        sessions.deleteByUser(user);
        return issueSession(user);
    }
    public void logout(String token) { if(token!=null && !token.isBlank()) sessions.deleteByTokenHash(tokens.hash(token)); }
    private ResponseStatusException error(HttpStatus status,String message){ return new ResponseStatusException(status,message); }
}
