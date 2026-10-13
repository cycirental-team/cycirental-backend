package kr.ac.cyci.deptrental.item;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ItemRepositoryTest {

    @Autowired
    ItemRepository items;

    @Test
    @DisplayName("조건 없이 조회하면 전체 6개")
    void searchAll() {
        assertEquals(6, items.search(null, null, false).size());
    }

    @Test
    @DisplayName("대여 가능만 보면 4개 (카메라, 노트북 제외)")
    void searchAvailableOnly() {
        assertEquals(4, items.search(null, null, true).size());
    }

    @Test
    @DisplayName("촬영/영상 카테고리는 3개")
    void searchByCategory() {
        assertEquals(3, items.search(null, "촬영/영상", false).size());
    }

    @Test
    @DisplayName("촬영/영상 + 대여 가능은 2개 (삼각대, 조명 세트)")
    void searchByCategoryAndAvailable() {
        assertEquals(2, items.search(null, "촬영/영상", true).size());
    }

    @Test
    @DisplayName("이름 검색은 대소문자 무시 (vr → VR 기기)")
    void searchByNameIgnoreCase() {
        assertEquals(1, items.search("vr", null, false).size());
    }

    @Test
    @DisplayName("카테고리 목록은 3개")
    void categories() {
        assertEquals(3, items.findAllCategories().size());
    }
}