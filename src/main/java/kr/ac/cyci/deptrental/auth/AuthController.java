package kr.ac.cyci.deptrental.auth;
import jakarta.validation.Valid;
import kr.ac.cyci.deptrental.auth.dto.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/auth")
public class AuthController {
    private final AuthService auth;
    public AuthController(AuthService auth){this.auth=auth;}
    @PostMapping("/signup") @ResponseStatus(HttpStatus.CREATED)
    public void signup(@Valid @RequestBody SignUpRequest request){auth.signUp(request);}
    @PostMapping("/login") public AuthResponse login(@Valid @RequestBody LoginRequest request){return auth.login(request);}
    @PostMapping("/logout") @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@RequestHeader(value="Authorization",required=false) String header){auth.logout(token(header));}
    @PostMapping("/password-reset/requests") @ResponseStatus(HttpStatus.ACCEPTED)
    public void requestReset(@Valid @RequestBody PasswordResetRequest request){auth.requestPasswordReset(request);}
    @PostMapping("/password/change")
    public AuthResponse change(@RequestHeader(value="Authorization",required=false) String header,@Valid @RequestBody ChangePasswordRequest request){return auth.changePassword(token(header),request);}
    // Old email/token reset is disabled; names and student numbers alone cannot reset passwords.
    @PostMapping("/password-reset") @ResponseStatus(HttpStatus.GONE)
    public void oldReset(){ }
    static String token(String header){return header!=null && header.startsWith("Bearer ")?header.substring(7):null;}
}
