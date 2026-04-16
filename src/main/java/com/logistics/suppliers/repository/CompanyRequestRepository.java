package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.CompanyRequest;
import com.logistics.suppliers.model.RequestStatus;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CompanyRequestRepository extends JpaRepository<CompanyRequest, Long> {
    List<CompanyRequest> findByCompanyAndStatus(Company company, RequestStatus status);
    boolean existsByUserAndCompanyAndStatus(User user, Company company, RequestStatus status);

    List<CompanyRequest> findByUserAndStatus(User user, RequestStatus requestStatus);
}
