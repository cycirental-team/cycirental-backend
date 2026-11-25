package kr.ac.cyci.deptrental.notice.dto;

import java.time.Instant;
import kr.ac.cyci.deptrental.notice.Notice;

public record NoticeResponse(Long id, String title, String content, boolean important,
                             Instant createdAt, Instant updatedAt) {
    public static NoticeResponse from(Notice notice) {
        return new NoticeResponse(notice.getId(), notice.getTitle(), notice.getContent(),
                notice.isImportant(), notice.getCreatedAt(), notice.getUpdatedAt());
    }
}
