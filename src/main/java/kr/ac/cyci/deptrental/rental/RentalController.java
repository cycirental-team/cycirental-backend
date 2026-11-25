package kr.ac.cyci.deptrental.rental;

import jakarta.validation.Valid;
import java.util.List;
import kr.ac.cyci.deptrental.rental.dto.RentalRequest;
import kr.ac.cyci.deptrental.rental.dto.RentalResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/rentals")
public class RentalController {
    private final RentalService service;

    public RentalController(RentalService service) {
        this.service = service;
    }

    // 대여 신청: POST /api/rentals
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public RentalResponse apply(@RequestHeader(value = "Authorization", required = false) String header,
                                @Valid @RequestBody RentalRequest request) {
        return service.apply(token(header), request);
    }

    // 내 대여 목록: GET /api/rentals/me
    @GetMapping("/me")
    public List<RentalResponse> myRentals(@RequestHeader(value = "Authorization", required = false) String header) {
        return service.myRentals(token(header));
    }

    // 신청 취소: POST /api/rentals/{id}/cancel
    @PostMapping("/{id}/cancel")
    public RentalResponse cancel(@RequestHeader(value = "Authorization", required = false) String header,
                                 @PathVariable Long id) {
        return service.cancel(token(header), id);
    }
    // 반납 요청: POST /api/rentals/{id}/return-request
    @PostMapping("/{id}/return-request")
    public RentalResponse requestReturn(@RequestHeader(value = "Authorization", required = false) String header,
                                        @PathVariable Long id) {
        return service.requestReturn(token(header), id);
    }

    // "Bearer abc123" 에서 앞의 "Bearer "를 떼고 토큰(abc123)만 꺼냄
    private static String token(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}