package kr.ac.cyci.deptrental.item;

import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/items")
public class ItemController {
    private final ItemRepository repository;

    public ItemController(ItemRepository repository) {
        this.repository = repository;
    }

    // 목록 조회: /api/items?query=VR&category=촬영/영상&availableOnly=true
    @GetMapping
    public List<Item> list(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "false") boolean availableOnly) {
        return repository.search(blankToNull(query), blankToNull(category), availableOnly);
    }

    // 카테고리 목록: /api/items/categories
    @GetMapping("/categories")
    public List<String> categories() {
        return repository.findAllCategories();
    }

    // 상세 조회: /api/items/1
    @GetMapping("/{id}")
    public Item detail(@PathVariable Long id) {
        return repository.findById(id).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not found"));
    }

    // 빈칸("")으로 오면 "안 보낸 것"으로 처리
    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}