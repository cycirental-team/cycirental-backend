package kr.ac.cyci.deptrental.item;

import jakarta.validation.Valid;
import kr.ac.cyci.deptrental.auth.AuthService;
import kr.ac.cyci.deptrental.item.dto.AdminItemRequest;
import kr.ac.cyci.deptrental.rental.RentalRepository;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.transaction.annotation.Transactional;

@RestController
@RequestMapping("/api/admin/items")
public class AdminItemController {
    private final ItemRepository items;
    private final RentalRepository rentals;
    private final AuthService auth;

    public AdminItemController(ItemRepository items, RentalRepository rentals, AuthService auth) {
        this.items = items;
        this.rentals = rentals;
        this.auth = auth;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Item create(@RequestHeader(value = "Authorization", required = false) String header,
                       @Valid @RequestBody AdminItemRequest request) {
        auth.requireAdmin(token(header));
        validateQuantities(request, null);
        return items.save(new Item(clean(request.name()), clean(request.category()), clean(request.description()),
                request.totalQuantity(), request.availableQuantity()));
    }

    @PutMapping("/{id}")
    @Transactional
    public Item update(@RequestHeader(value = "Authorization", required = false) String header,
                       @PathVariable Long id, @Valid @RequestBody AdminItemRequest request) {
        auth.requireAdmin(token(header));
        Item item = items.lockById(id).orElseThrow(() -> notFound(id));
        int rentedCount = item.getTotalQuantity() - item.getAvailableQuantity();
        validateQuantities(request, rentedCount);
        item.update(clean(request.name()), clean(request.category()), clean(request.description()),
                request.totalQuantity(), request.availableQuantity());
        return items.save(item);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@RequestHeader(value = "Authorization", required = false) String header,
                       @PathVariable Long id) {
        auth.requireAdmin(token(header));
        Item item = items.findById(id).orElseThrow(() -> notFound(id));
        if (rentals.existsByItemId(id)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "대여 이력이 있는 물품은 삭제할 수 없습니다. 수량을 0으로 조정하거나 비활성화 기능을 사용하세요.");
        }
        items.delete(item);
    }

    private static void validateQuantities(AdminItemRequest request, Integer rentedCount) {
        if (request.availableQuantity() > request.totalQuantity()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "대여 가능 수량은 전체 수량보다 클 수 없습니다.");
        }
        if (rentedCount != null && request.totalQuantity() - request.availableQuantity() != rentedCount) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "대여 중인 수량은 변경할 수 없습니다. 전체 수량과 대여 가능 수량을 함께 조정해 주세요.");
        }
    }

    private static String token(String header) {
        return header != null && header.startsWith("Bearer ") ? header.substring(7) : null;
    }

    private static String clean(String value) { return value.trim(); }
    private static ResponseStatusException notFound(Long id) {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "물품을 찾을 수 없습니다: " + id);
    }
}
