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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/companies")
@RequiredArgsConstructor
public class CompanyController {

    private final CompanyService companyService;
    private final UserRepository userRepository;
    private final CompanyRequestRepository requestRepository;
    private final CompanyRepository companyRepository;

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
    public String joinCompany(@PathVariable Long id,
                              Authentication authentication,
                              RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName()).get();

        try {
            companyService.createJoinRequest(user, id);
            redirectAttributes.addFlashAttribute("message", "Заявка успешно отправлена! Ожидайте подтверждения владельцем.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }

        return "redirect:/companies";
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

        if (company == null) return "redirect:/companies";

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());

        model.addAttribute("company", company);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentUser", currentUser); // Передаем текущего юзера для профиля

        model.addAttribute("employees", userRepository.findByCompany(company));

        if (isOwner) {
            model.addAttribute("requests", requestRepository.findByCompanyAndStatus(company, RequestStatus.CREATED));
        }

        return "company-profile";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, Authentication authentication) {
        CompanyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заявка не найдена"));

        User user = request.getUser();
        user.setCompany(request.getCompany());
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

    @PostMapping("/leave")
    public String leaveCompany(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        try {
            companyService.leaveCompany(user);
            redirectAttributes.addFlashAttribute("message", "Вы успешно покинули компанию.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/companies";
    }

    @PostMapping("/transfer-ownership/{newOwnerId}")
    public String transferOwnership(@PathVariable Long newOwnerId, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        Company company = currentUser.getCompany();

        if (company.getOwner().getId().equals(currentUser.getId())) {
            User newOwner = userRepository.findById(newOwnerId)
                    .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

            if (!newOwner.getCompany().getId().equals(company.getId())) {
                throw new RuntimeException("Пользователь должен быть сотрудником вашей компании");
            }

            company.setOwner(newOwner);
            companyRepository.save(company);
        }

        return "redirect:/companies/my?transferred=true";
    }
}