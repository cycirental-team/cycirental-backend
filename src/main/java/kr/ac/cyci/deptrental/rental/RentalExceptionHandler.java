package kr.ac.cyci.deptrental.rental;

import java.util.LinkedHashMap;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;

// 대여 API에서 오류가 나면 앱이 읽을 수 있는 안내 문구(detail)로 바꿔서 돌려줌
// (AuthExceptionHandler와 같은 방식, 대여 컨트롤러에만 적용)
@RestControllerAdvice(assignableTypes = RentalController.class)
public class RentalExceptionHandler {

    // 입력값이 비었을 때 (예: 반납 예정일을 안 보냄)
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail invalidInput(MethodArgumentNotValidException exception) {
        var errors = new LinkedHashMap<String, String>();
        for (var error : exception.getBindingResult().getFieldErrors()) {
            errors.putIfAbsent(error.getField(), error.getDefaultMessage());
        }
        var detail = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                errors.isEmpty() ? "입력값을 확인해 주세요." : String.join("\n", errors.values()));
        detail.setProperty("errors", errors);
        return detail;
    }

    // 보낸 데이터 모양이 틀렸을 때 (예: 날짜를 "내일"처럼 글자로 보냄)
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ProblemDetail invalidBody(HttpMessageNotReadableException exception) {
        return ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "요청 데이터 형식을 확인해 주세요.");
    }

    // RentalService가 막은 경우 (로그인 안 함, 날짜 범위, 재고 없음, 중복, 취소 불가 등)
    @ExceptionHandler(ResponseStatusException.class)
    public ProblemDetail rejectedRequest(ResponseStatusException exception) {
        return ProblemDetail.forStatusAndDetail(exception.getStatusCode(),
                exception.getReason() == null ? "요청을 처리하지 못했습니다." : exception.getReason());
    }
}