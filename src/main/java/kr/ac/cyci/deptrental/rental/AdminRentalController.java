package kr.ac.cyci.deptrental.rental;

import java.util.List;
import kr.ac.cyci.deptrental.rental.dto.AdminRentalResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/rentals")
public class AdminRentalController {
    private final AdminRentalService service;

    public AdminRentalController(AdminRentalService service) {
        this.service = service;
    }

    // 관리자 대여 목록: GET /api/admin/rentals?status=PENDING (status 생략 시 전체)
    @GetMapping
    public List<AdminRentalResponse> list(@RequestHeader(value = "Authorization", required = false) String header,
                                          @RequestParam(required = false) RentalStatus status) {
        return service.list(token(header), status);
    }
    // 대여 승인: POST /api/admin/rentals/{id}/approve
    @PostMapping("/{id}/approve")
    public AdminRentalResponse approve(@RequestHeader(value = "Authorization", required = false) String header,
                                       @PathVariable Long id) {
        return service.approve(token(header), id);
    }

    // "Bearer abc123" 에서 앞의 "Bearer "를 떼고 토큰(abc123)만 꺼냄
    private static String token(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}