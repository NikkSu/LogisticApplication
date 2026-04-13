package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.CompanyRequestRepository;
import com.logistics.suppliers.service.CompanyService;
import com.logistics.suppliers.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final CompanyRequestRepository requestRepository;

    @GetMapping
    public String listCompanies(Model model, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();

        if (currentUser.getCompany() != null) {
            model.addAttribute("userCompany", currentUser.getCompany());
            return "companies";
        }

        model.addAttribute("suppliers", companyService.getCompaniesByType(CompanyType.SUPPLIER));
        model.addAttribute("customers", companyService.getCompaniesByType(CompanyType.CUSTOMER));
        return "companies";
    }

    @PostMapping("/join/{id}")
    public String joinCompany(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();

        if (user.getCompany() != null) {
            return "redirect:/companies?error=Вы уже состоите в компании " + user.getCompany().getName();
        }

        try {
            companyService.createJoinRequest(user, id);
            return "redirect:/companies?success=true";
        } catch (Exception e) {
            return "redirect:/companies?error=" + e.getMessage();
        }
    }

    @GetMapping("/create")
    public String createCompanyForm(Model model) {
        model.addAttribute("company", new Company());
        model.addAttribute("types", CompanyType.values());
        return "company-create";
    }

    @PostMapping("/create")
    public String createCompanySubmit(@ModelAttribute Company company, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

        company.setOwner(user);
        Company savedCompany = companyService.saveCompany(company);

        user.setCompany(savedCompany);

        if (savedCompany.getType() == CompanyType.SUPPLIER) {
            user.setRole(Role.SUPPLIER);
        } else {
            user.setRole(Role.MANAGER);
        }

        userRepository.save(user);

        return "redirect:/dashboard?companyCreated=true";
    }
    @GetMapping("/my")
    public String myCompany(Model model, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        Company company = currentUser.getCompany();

        if (company == null) {
            return "redirect:/companies";
        }

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());

        model.addAttribute("company", company);
        model.addAttribute("isOwner", isOwner);

        if (isOwner) {
            List<CompanyRequest> requests = requestRepository.findByCompanyAndStatus(company, RequestStatus.CREATED);
            model.addAttribute("requests", requests);

            List<User> employees = userRepository.findByCompany(company);
            model.addAttribute("employees", employees);
        }

        return "company-profile";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, Authentication authentication) {
        CompanyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        // Логика одобрения
        User user = request.getUser();
        user.setCompany(request.getCompany());
        // Назначаем роль в зависимости от типа компании
        if (request.getCompany().getType() == CompanyType.SUPPLIER) {
            user.setRole(Role.SUPPLIER);
        } else {
            user.setRole(Role.MANAGER);
        }
        userRepository.save(user);

        request.setStatus(RequestStatus.APPROVED);
        requestRepository.save(request);

        return "redirect:/companies/my?approved=true";
    }

    @PostMapping("/requests/{id}/reject")
    public String rejectRequest(@PathVariable Long id) {
        CompanyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        request.setStatus(RequestStatus.REJECTED);
        requestRepository.save(request);

        return "redirect:/companies/my?rejected=true";
    }


}