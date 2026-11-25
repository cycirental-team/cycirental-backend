package kr.ac.cyci.deptrental.notice;

import java.util.List;
import kr.ac.cyci.deptrental.notice.dto.NoticeResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notices")
public class NoticeController {
    private final NoticeService service;

    public NoticeController(NoticeService service) { this.service = service; }

    @GetMapping
    public List<NoticeResponse> list() { return service.list(); }

    @GetMapping("/{id}")
    public NoticeResponse detail(@PathVariable Long id) { return service.detail(id); }
}
