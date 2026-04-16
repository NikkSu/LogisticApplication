package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.exceptions.ResourceNotFoundException;
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
        User currentUser = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = currentUser.getCompany();

        if (company == null) return "redirect:/companies";

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId());
        boolean canManage = isOwner || currentUser.isCanManageEmployees();

        model.addAttribute("company", company);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("canManage", canManage);
        model.addAttribute("employees", userRepository.findByCompany(company));

        if (canManage) {
            model.addAttribute("requests", requestRepository.findByCompanyAndStatus(company, RequestStatus.CREATED));
        }

        return "company-profile";
    }

    @PostMapping("/requests/{id}/approve")
    public String approveRequest(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        CompanyRequest request = requestRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Заявка не найдена"));

        User user = request.getUser();

        if (user.getCompany() != null) {
            request.setStatus(RequestStatus.REJECTED);
            requestRepository.save(request);

            redirectAttributes.addFlashAttribute("message", "Пользователь уже вступил в другую компанию. Заявка отклонена.");
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/companies/my";
        }

        user.setCompany(request.getCompany());
        user.setRole(request.getCompany().getType() == CompanyType.SUPPLIER ? Role.SUPPLIER : Role.MANAGER);
        userRepository.save(user);

        request.setStatus(RequestStatus.APPROVED);
        requestRepository.save(request);

        List<CompanyRequest> otherRequests = requestRepository.findByUserAndStatus(user, RequestStatus.CREATED);
        for (CompanyRequest other : otherRequests) {
            other.setStatus(RequestStatus.REJECTED);
        }
        requestRepository.saveAll(otherRequests);

        redirectAttributes.addFlashAttribute("message", "Сотрудник успешно принят!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/companies/my";
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

    @PostMapping("/employees/{id}/kick")
    public String kickEmployee(@PathVariable Long id, Authentication authentication, RedirectAttributes redirectAttributes) {
        User owner = userRepository.findByEmail(authentication.getName()).get();
        User employee = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Сотрудник не найден"));

        if (owner.getCompany().getOwner().getId().equals(owner.getId()) || owner.isCanManageEmployees()) {
            if (employee.getId().equals(owner.getId())) {
                redirectAttributes.addFlashAttribute("message", "Вы не можете уволить самого себя.");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/companies/my";
            }

            employee.setCompany(null);
            employee.setRole(Role.MANAGER);
            userRepository.save(employee);

            redirectAttributes.addFlashAttribute("message", "Сотрудник исключен из компании.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        return "redirect:/companies/my";
    }

    @PostMapping("/employees/{id}/toggle-permission")
    public String togglePermission(@PathVariable Long id, Authentication authentication) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        User targetUser = userRepository.findById(id).orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        if (currentUser.getCompany().getOwner().getId().equals(currentUser.getId())) {
            targetUser.setCanManageEmployees(!targetUser.isCanManageEmployees());
            userRepository.save(targetUser);
        }

        return "redirect:/companies/my";
    }
}