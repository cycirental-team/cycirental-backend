package kr.ac.cyci.deptrental.item;

import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ItemRepository extends JpaRepository<Item, Long> {

    // 조건대로 물품 골라 오기 (조건이 비어 있으면 그 조건은 무시)
    @Query("""
            select i from Item i
            where (:query is null or lower(i.name) like lower(concat('%', :query, '%')))
              and (:category is null or i.category = :category)
              and (:availableOnly = false or i.availableQuantity > 0)
            order by i.id
            """)
    List<Item> search(@Param("query") String query,
                      @Param("category") String category,
                      @Param("availableOnly") boolean availableOnly);

    // 카테고리 목록 가져오기 (앱 위쪽 버튼용)
    @Query("select distinct i.category from Item i order by i.category")
    List<String> findAllCategories();
}