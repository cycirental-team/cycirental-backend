package kr.ac.cyci.deptrental.item;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository items;

    @Test
    @DisplayName("조건 없이 조회하면 전체 물품이 나온다")
    void searchAll() {
        assertEquals(items.count(), items.search(null, null, false).size());
    }

    @Test
    @DisplayName("대여 가능만 보면 남은 수량이 1 이상인 물품만 나온다")
    void searchAvailableOnly() {
        List<Item> result = items.search(null, null, true);
        long expected = items.findAll().stream()
                .filter(item -> item.getAvailableQuantity() > 0)
                .count();
        assertEquals(expected, result.size());
        assertTrue(result.stream().allMatch(item -> item.getAvailableQuantity() > 0));
    }

    @Test
    @DisplayName("카테고리를 고르면 그 카테고리 물품만 나온다")
    void searchByCategory() {
        List<Item> result = items.search(null, "전자기기", false);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(item -> item.getCategory().equals("전자기기")));
    }

    @Test
    @DisplayName("카테고리와 대여 가능을 같이 쓰면 두 조건을 모두 만족한다")
    void searchByCategoryAndAvailable() {
        List<Item> result = items.search(null, "전자기기", true);
        assertTrue(result.stream().allMatch(item ->
                item.getCategory().equals("전자기기") && item.getAvailableQuantity() > 0));
    }

    @Test
    @DisplayName("이름 검색은 대소문자를 무시한다 (vr → VR 기기)")
    void searchByNameIgnoreCase() {
        List<Item> result = items.search("vr", null, false);
        assertFalse(result.isEmpty());
        assertTrue(result.stream().allMatch(item -> item.getName().toLowerCase().contains("vr")));
    }

    @Test
    @DisplayName("카테고리 목록은 중복 없이 모든 카테고리를 담는다")
    void categories() {
        long expected = items.findAll().stream().map(Item::getCategory).distinct().count();
        assertEquals(expected, items.findAllCategories().size());
    }
}