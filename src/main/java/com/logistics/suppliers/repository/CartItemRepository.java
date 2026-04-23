package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.CartItem;
import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CartItemRepository extends JpaRepository<CartItem, Long> {
    List<CartItem> findByCompany(Company company);
    Optional<CartItem> findByCompanyAndProduct(Company company, Product product);
    void deleteByCompany(Company company);
}