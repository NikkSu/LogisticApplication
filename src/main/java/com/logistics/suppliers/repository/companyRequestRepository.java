package com.logistics.suppliers.repository;

import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.CompanyRequest;
import com.logistics.suppliers.model.RequestStatus;
import com.logistics.suppliers.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface companyRequestRepository extends JpaRepository<CompanyRequest, Long> {

    boolean existsByUserAndCompanyAndStatus(User user, Company company, RequestStatus status);
}
