package kr.ac.cyci.deptrental.notice;

import jakarta.validation.Valid;
import kr.ac.cyci.deptrental.notice.dto.NoticeRequest;
import kr.ac.cyci.deptrental.notice.dto.NoticeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/notices")
public class AdminNoticeController {
    private final NoticeService service;

    public AdminNoticeController(NoticeService service) { this.service = service; }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoticeResponse create(@RequestHeader(value = "Authorization", required = false) String header,
                                 @Valid @RequestBody NoticeRequest request) {
        return service.create(token(header), request);
    }

    @PutMapping("/{id}")
    public NoticeResponse update(@RequestHeader(value = "Authorization", required = false) String header,
                                 @PathVariable Long id, @Valid @RequestBody NoticeRequest request) {
        return service.update(token(header), id, request);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "Authorization", required = false) String header,
                       @PathVariable Long id) {
        service.delete(token(header), id);
    }

    private static String token(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }
}
