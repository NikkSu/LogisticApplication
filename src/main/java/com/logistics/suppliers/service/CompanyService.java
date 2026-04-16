package com.logistics.suppliers.service;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.CompanyRequestRepository;
import com.logistics.suppliers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CompanyService {
    private final CompanyRepository companyRepository;
    private final CompanyRequestRepository requestRepository;
    private final UserRepository userRepository;

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

    @Transactional
    public void leaveCompany(User user) {
        Company company = user.getCompany();
        if (company == null) return;

        if (company.getOwner() != null && company.getOwner().getId().equals(user.getId())) {
            List<User> members = userRepository.findByCompany(company);
            if (members.size() > 1) {
                throw new RuntimeException("Вы владелец. Сначала передайте права другому сотруднику.");
            }
            company.setOwner(null);
            companyRepository.save(company);
        }

        user.setCompany(null);
        user.setRole(Role.MANAGER);
        userRepository.save(user);
    }
}