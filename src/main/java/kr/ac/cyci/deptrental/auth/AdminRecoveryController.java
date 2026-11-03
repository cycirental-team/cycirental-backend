package kr.ac.cyci.deptrental.auth;
import java.util.List;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/admin/password-reset-requests")
public class AdminRecoveryController {
    private final AuthService auth;
    public AdminRecoveryController(AuthService auth){this.auth=auth;}
    @GetMapping public List<AuthService.RecoveryView> list(@RequestHeader(value="Authorization",required=false) String header){return auth.listRecovery(AuthController.token(header));}
    public record Approval(boolean identityConfirmed) { }
    @PostMapping("/{id}/approve") public AuthService.TemporaryPassword approve(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id,@RequestBody Approval body){return auth.processRecovery(AuthController.token(header),id,true,body.identityConfirmed());}
    @PostMapping("/{id}/reject") public void reject(@RequestHeader(value="Authorization",required=false) String header,@PathVariable Long id){auth.processRecovery(AuthController.token(header),id,false,false);}
}
