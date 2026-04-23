package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Order;
import com.logistics.suppliers.model.OrderItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
    List<OrderItem> findByOrder(Order order);
}
