package kr.ac.cyci.deptrental;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import kr.ac.cyci.deptrental.item.ItemRepository;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class DeptRentalApiApplicationTests {
	@Autowired
	ItemRepository items;

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

}
