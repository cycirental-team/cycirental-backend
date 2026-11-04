package kr.ac.cyci.deptrental.rental;

public enum RentalStatus {
    PENDING,          // 승인 대기 (학생이 신청함)
    RENTED,           // 대여 중 (관리자가 승인함)
    RETURN_REQUESTED, // 반납 확인 대기 (학생이 반납 요청함)
    RETURNED,         // 반납 완료 (관리자가 실물 확인함)
    REJECTED,         // 거절됨
    CANCELLED,        // 학생이 취소함
    EXPIRED           // 승인 안 된 채 만료됨
}