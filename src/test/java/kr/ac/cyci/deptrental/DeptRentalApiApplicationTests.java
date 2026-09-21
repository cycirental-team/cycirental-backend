package kr.ac.cyci.deptrental;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
class DeptRentalApiApplicationTests {
	@Autowired
	ItemRepository items;

	@Test
	void contextLoads() {
		assertEquals(6, items.count());
	}

}
