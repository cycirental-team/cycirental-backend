package kr.ac.cyci.deptrental;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.auth.dto.AuthResponse;
import kr.ac.cyci.deptrental.auth.dto.LoginRequest;
import kr.ac.cyci.deptrental.auth.dto.SignUpRequest;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DeptRentalApiApplicationTests {
	@Autowired
	ItemRepository items;

	@Autowired
	AuthService authService;

	@Test
	void contextLoads() {
		assertEquals(6, items.count());
	}

	@Test
	void signUpAndLoginUseLoginIdWhileUserIdRemainsNumericPrimaryKey() {
		authService.signUp(new SignUpRequest(
				"new_student", "password123", "테스트 학생", "student@example.com"));

		AuthResponse response = authService.login(new LoginRequest("new_student", "password123"));

		assertNotNull(response.userId());
		assertEquals("new_student", response.loginId());
		assertEquals("테스트 학생", response.displayName());
	}

}
