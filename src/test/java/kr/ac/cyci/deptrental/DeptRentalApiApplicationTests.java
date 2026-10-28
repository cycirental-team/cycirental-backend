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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class DeptRentalApiApplicationTests {
	@Autowired
	ItemRepository items;

	@Autowired
	AuthService authService;

	@Test
	void contextLoads() {
		var catalog = items.findAll();
		assertEquals(5, catalog.size());
		assertTrue(catalog.stream().anyMatch(item ->
				item.getName().equals("4090본체") &&
				item.getDescription().contains("2210호") &&
				item.getTotalQuantity() == 18 &&
				item.getAvailableQuantity() == 18));
		assertTrue(catalog.stream().anyMatch(item ->
				item.getName().equals("4090본체") &&
				item.getDescription().contains("2211호") &&
				item.getTotalQuantity() == 27 &&
				item.getAvailableQuantity() == 27));
		assertTrue(catalog.stream().anyMatch(item ->
				item.getName().equals("VR 기기") &&
				item.getDescription().contains("3401호") &&
				item.getTotalQuantity() == 3 &&
				item.getAvailableQuantity() == 3));
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
