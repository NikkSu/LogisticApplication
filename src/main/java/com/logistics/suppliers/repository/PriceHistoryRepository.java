package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Category;
import com.logistics.suppliers.model.PriceHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PriceHistoryRepository extends JpaRepository<PriceHistory, Long> {

    List<PriceHistory> findByProductIdOrderByChangedAtAsc(Long productId);
}
