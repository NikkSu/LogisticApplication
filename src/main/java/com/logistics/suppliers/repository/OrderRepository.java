package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface OrderRepository extends JpaRepository<Order, Long> {
    List<Order> findByCustomerCompanyOrderByCreatedAtDesc(Company company);

    List<Order> findBySupplierCompanyOrderByCreatedAtDesc(Company company);

    long countByCreatedBy(User user);

    List<Order> findByCustomerCompanyAndStatus(Company company, OrderStatus status);

    List<Order> findBySupplierCompanyAndStatus(Company company, OrderStatus status);

    long countByCustomerCompanyAndStatusIn(Company company, Collection<OrderStatus> statuses);
    long countBySupplierCompanyAndStatusIn(Company company, Collection<OrderStatus> statuses);

    List<Order> findAllByCustomerCompany(Company company);

    List<Order> findBySupplierCompany(Company company);
}
