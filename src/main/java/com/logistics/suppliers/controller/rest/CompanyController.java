package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.*;
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

    @GetMapping
    public String listCompanies(Model model) {
        model.addAttribute("suppliers", companyService.getCompaniesByType(CompanyType.SUPPLIER));
        model.addAttribute("customers", companyService.getCompaniesByType(CompanyType.CUSTOMER));
        return "companies";
    }

    @PostMapping("/join/{id}")
    public String joinCompany(@PathVariable Long id, Authentication authentication) {
        if (authentication == null) return "redirect:/login";

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));

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
//    @GetMapping("/my")
//    public String myCompany(Model model, Authentication authentication) {
//        User currentUser = userRepository.findByEmail(authentication.getName()).get();
//        Company company = currentUser.getCompany();
//
//        if (company == null) {
//            return "redirect:/companies";
//        }
//
//        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
//
//        model.addAttribute("company", company);
//        model.addAttribute("isOwner", isOwner);
//
//        if (isOwner) {
//            List<CompanyRequest> requests = requestRepository.findByCompanyAndStatus(company, RequestStatus.CREATED);
//            model.addAttribute("requests", requests);
//
//            List<User> employees = userRepository.findByCompany(company);
//            model.addAttribute("employees", employees);
//        }
//
//        return "company-profile";
//    }
}