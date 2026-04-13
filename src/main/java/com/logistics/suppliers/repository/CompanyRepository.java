package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.CompanyType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRepository extends JpaRepository<Company, Long> {
    List<Company> findByType(CompanyType type);

}