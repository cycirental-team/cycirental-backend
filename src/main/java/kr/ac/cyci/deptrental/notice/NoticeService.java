package kr.ac.cyci.deptrental.notice;

import java.util.List;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.notice.dto.NoticeRequest;
import kr.ac.cyci.deptrental.notice.dto.NoticeResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
@Transactional(readOnly = true)
public class NoticeService {
    private final NoticeRepository notices;
    private final AuthService auth;

    public NoticeService(NoticeRepository notices, AuthService auth) {
        this.notices = notices;
        this.auth = auth;
    }

    public List<NoticeResponse> list() {
        return notices.findAllByOrderByImportantDescCreatedAtDescIdDesc().stream()
                .map(NoticeResponse::from)
                .toList();
    }

    public NoticeResponse detail(Long id) {
        return NoticeResponse.from(find(id));
    }

    @Transactional
    public NoticeResponse create(String token, NoticeRequest request) {
        auth.requireAdmin(token);
        Notice notice = notices.save(new Notice(clean(request.title()), clean(request.content()), request.important()));
        return NoticeResponse.from(notice);
    }

    @Transactional
    public NoticeResponse update(String token, Long id, NoticeRequest request) {
        auth.requireAdmin(token);
        Notice notice = find(id);
        notice.update(clean(request.title()), clean(request.content()), request.important());
        return NoticeResponse.from(notice);
    }

    @Transactional
    public void delete(String token, Long id) {
        auth.requireAdmin(token);
        notices.delete(find(id));
    }

    private Notice find(Long id) {
        return notices.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다: " + id));
    }

    private static String clean(String value) { return value.trim(); }
}
