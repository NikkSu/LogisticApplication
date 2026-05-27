package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.exceptions.ResourceNotFoundException;
import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.CompanyRequestRepository;
import com.logistics.suppliers.repository.ProductRepository;
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
    private final ProductRepository productRepository;

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
            redirectAttributes.addFlashAttribute("message", "Вы покинули компанию. Она была удалена, так как в ней не осталось участников.");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/companies";
    }

    @GetMapping("/view/{id}")
    public String viewCompany(@PathVariable Long id, Model model) {
        Company company = companyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Компания не найдена"));

        List<Product> companyProducts = productRepository.findBySupplier(company);

        model.addAttribute("company", company);
        model.addAttribute("products", companyProducts);
        return "company-view";
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
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        User targetEmployee = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден"));
        Company company = currentUser.getCompany();

        boolean isOwner = company.getOwner().getId().equals(currentUser.getId());
        boolean canManage = currentUser.isCanManageEmployees();

        if (isOwner || canManage) {
            if (targetEmployee.getId().equals(currentUser.getId())) {
                redirectAttributes.addFlashAttribute("message", "Вы не можете исключить себя.");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/companies/my";
            }

            if (!isOwner && targetEmployee.getId().equals(company.getOwner().getId())) {
                redirectAttributes.addFlashAttribute("message", "У вас нет прав исключать владельца.");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/companies/my";
            }

            if (!isOwner && targetEmployee.isCanManageEmployees()) {
                redirectAttributes.addFlashAttribute("message", "Вы не можете исключать других администраторов.");
                redirectAttributes.addFlashAttribute("messageType", "error");
                return "redirect:/companies/my";
            }

            targetEmployee.setCompany(null);
            targetEmployee.setRole(Role.MANAGER);
            targetEmployee.setCanManageEmployees(false);
            userRepository.save(targetEmployee);

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

    @PostMapping("/employees/{id}/change-role")
    public String changeEmployeeRole(@PathVariable Long id,
                                     @RequestParam Role newRole,
                                     Authentication authentication,
                                     RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        User targetEmployee = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Сотрудник не найден"));

        if (currentUser.getCompany().getOwner().getId().equals(currentUser.getId())) {

            if (targetEmployee.getId().equals(currentUser.getId())) {
                return "redirect:/companies/my";
            }

            targetEmployee.setRole(newRole);
            userRepository.save(targetEmployee);

            redirectAttributes.addFlashAttribute("message", "Роль сотрудника успешно изменена на " + newRole);
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        return "redirect:/companies/my";
    }

    @PostMapping("/my/update")
    public String updateCompanyDetails(@ModelAttribute Company companyData,
                                       Authentication authentication,
                                       RedirectAttributes redirectAttributes) {
        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        Company company = currentUser.getCompany();
        
        boolean canManage = company.getOwner() != null && company.getOwner().getId().equals(currentUser.getId())
                || currentUser.isCanManageEmployees();

        if (canManage) {
            company.setName(companyData.getName());
            company.setDescription(companyData.getDescription());
            company.setAddress(companyData.getAddress());
            company.setContactEmail(companyData.getContactEmail());
            company.setLogoUrl(companyData.getLogoUrl());

            companyRepository.save(company);
            redirectAttributes.addFlashAttribute("message", "Данные компании обновлены!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        }

        return "redirect:/companies/my";
    }
}