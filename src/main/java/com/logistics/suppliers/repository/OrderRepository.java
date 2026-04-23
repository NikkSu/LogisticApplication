package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.Order;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerCompanyOrderByCreatedAtDesc(Company company);

    List<Order> findBySupplierCompanyOrderByCreatedAtDesc(Company company);

    long countByCreatedBy(User user);
}
