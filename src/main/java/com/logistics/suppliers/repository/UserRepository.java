package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    List<User> findByCompany(Company company);
    Optional<User> findByEmailIgnoreCase(String email);
    @Query("SELECT CAST(u.createdAt AS date) as date, COUNT(u) as count FROM User u GROUP BY CAST(u.createdAt AS date) ORDER BY date ASC")
    List<Object[]> getUserRegistrationStats();
}