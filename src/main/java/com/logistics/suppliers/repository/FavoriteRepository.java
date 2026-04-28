package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Favorite;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    Optional<Favorite> findByUserAndProduct(User user, Product product);
    List<Favorite> findByUser(User user);
    long countByUser(User user);
    boolean existsByUserAndProduct(User user, Product product);
}