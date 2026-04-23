package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Order;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OrderRepository extends JpaRepository<Order, Long> {

    long countByCreatedBy(User user);
}
