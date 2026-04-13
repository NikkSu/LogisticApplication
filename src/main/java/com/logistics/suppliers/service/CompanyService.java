package com.logistics.suppliers.service;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.companyRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final companyRequestRepository requestRepository;

    public List<Company> getCompaniesByType(CompanyType type) {
        return companyRepository.findByType(type);
    }

    public void createJoinRequest(User user, Long companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Компания не найдена"));

        boolean exists = requestRepository.existsByUserAndCompanyAndStatus(user, company, RequestStatus.CREATED);
        if (exists) throw new RuntimeException("Заявка уже на рассмотрении");

        CompanyRequest request = new CompanyRequest();
        request.setUser(user);
        request.setCompany(company);
        request.setStatus(RequestStatus.CREATED);
        requestRepository.save(request);
    }
    public Company saveCompany(Company company) {
        return companyRepository.save(company);
    }
}