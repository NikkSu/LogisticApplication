package com.logistics.suppliers.controller.common;

import com.logistics.suppliers.exceptions.ResourceNotFoundException;
import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final CompanyRepository companyRepository;
    private final UserRepository userRepository;
    private final OrderRepository orderRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @GetMapping("/dashboard")
    public String adminDashboard(Model model) {
        BigDecimal turnover = orderRepository.getTotalTurnover();
        model.addAttribute("turnover", turnover != null ? turnover : BigDecimal.ZERO);
        model.addAttribute("totalCompanies", companyRepository.count());
        model.addAttribute("totalOrders", orderRepository.count());
        model.addAttribute("totalUsers", userRepository.count());

        model.addAttribute("latestOrders", orderRepository.findTop5ByOrderByCreatedAtDesc());

        List<Object[]> regStats = userRepository.getUserRegistrationStats();
        model.addAttribute("regLabels", regStats.stream().map(s -> s[0].toString()).toList());
        model.addAttribute("regValues", regStats.stream().map(s -> s[1]).toList());

        return "admin/dashboard";
    }

    @GetMapping("/companies")
    public String listCompanies(Model model) {
        model.addAttribute("companies", companyRepository.findAll());
        return "admin/companies";
    }

    @Transactional
    @PostMapping("/companies/delete/{id}")
    public String deleteCompany(@PathVariable Long id) {
        Company company = companyRepository.findById(id).orElseThrow();

        List<User> employees = userRepository.findByCompany(company);
        for (User emp : employees) {
            emp.setCompany(null);
            emp.setRole(Role.MANAGER);
            emp.setCanManageEmployees(false);
        }
        userRepository.saveAll(employees);

        List<Product> products = productRepository.findBySupplier(company);
        productRepository.deleteAll(products);

        companyRepository.delete(company);

        return "redirect:/admin/companies?success=deleted";
    }

    @GetMapping("/categories")
    public String listCategories(Model model) {
        model.addAttribute("categories", categoryRepository.findAll());
        return "admin/categories";
    }

    @Transactional
    @PostMapping("/categories/delete/{id}")
    public String deleteCategory(@PathVariable Long id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Категория не найдена"));

        List<Product> products = productRepository.findByCategoryId(id);

        for (Product p : products) {
            p.setCategory(null);
        }
        productRepository.saveAll(products);

        categoryRepository.delete(category);
        return "redirect:/admin/categories";
    }

    @PostMapping("/categories/save")
    public String saveCategory(@RequestParam String name) {
        Category category = new Category();
        category.setName(name);
        categoryRepository.save(category);
        return "redirect:/admin/categories";
    }
}