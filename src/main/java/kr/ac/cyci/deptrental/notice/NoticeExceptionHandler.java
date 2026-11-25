package kr.ac.cyci.deptrental.notice;

import java.util.LinkedHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice(assignableTypes = {NoticeController.class, AdminNoticeController.class})
public class NoticeExceptionHandler {
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidInput(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        exception.getBindingResult().getFieldErrors().forEach(error ->
                errors.putIfAbsent(error.getField(), error.getDefaultMessage()));
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                errors.isEmpty() ? "입력값을 확인해 주세요." : String.join("\n", errors.values()));
        detail.setProperty("errors", errors);
        return detail;
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidBody(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 데이터 형식을 확인해 주세요.");
    }

    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejectedRequest(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
                exception.getReason() == null ? "요청을 처리하지 못했습니다." : exception.getReason());
    }
}
