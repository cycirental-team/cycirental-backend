package kr.ac.cyci.deptrental.item;

import static org.junit.jupiter.api.Assertions.*;
import jakarta.validation.Validation;
import java.util.concurrent.*;
import kr.ac.cyci.deptrental.item.dto.AdminItemRequest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.support.TransactionTemplate;

@SpringBootTest
class AdminItemRegressionTest {
    @Autowired ItemRepository items;
    @Autowired TransactionTemplate tx;

    @Test
    void categoryMatchesDatabaseLimit() {
        try (var factory = Validation.buildDefaultValidatorFactory()) {
            var validator = factory.getValidator();
            assertTrue(validator.validate(new AdminItemRequest("item", "x".repeat(80), "place", 3, 3)).isEmpty());
            assertFalse(validator.validate(new AdminItemRequest("item", "x".repeat(81), "place", 3, 3)).isEmpty());
        }
    }

    @Test
    void itemLockSerializesStockDecreaseAndEdit() throws Exception {
        Long id = tx.execute(s -> items.saveAndFlush(new Item("lock test", "test", "place", 3, 3)).getId());
        var locked = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var executor = Executors.newFixedThreadPool(2);
        try {
            var edit = executor.submit(() -> tx.executeWithoutResult(s -> {
                Item item = items.lockById(id).orElseThrow();
                locked.countDown();
                try { if (!release.await(5, TimeUnit.SECONDS)) throw new IllegalStateException("timeout"); }
                catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException(e); }
                item.update("edited", item.getCategory(), item.getDescription(), 3, 3);
            }));
            assertTrue(locked.await(5, TimeUnit.SECONDS));
            var approval = executor.submit(() -> tx.execute(s -> items.decreaseAvailable(id)));
            release.countDown();
            edit.get(10, TimeUnit.SECONDS);
            assertEquals(1, approval.get(10, TimeUnit.SECONDS));
            assertEquals(2, items.findById(id).orElseThrow().getAvailableQuantity());
        } finally {
            release.countDown();
            executor.shutdownNow();
            if (!executor.awaitTermination(15, TimeUnit.SECONDS)) {
                throw new IllegalStateException("작업 스레드가 종료되지 않아 테스트 물품을 안전하게 정리할 수 없습니다.");
            }
            tx.executeWithoutResult(s -> items.deleteById(id));
        }
    }
}
