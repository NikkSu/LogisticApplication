package com.logistics.suppliers.controller.common;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.OrderRepository;
import com.logistics.suppliers.repository.ProductRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.AnalyticsService;
import com.logistics.suppliers.util.ExcelExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/analytics")
@RequiredArgsConstructor
public class AnalyticsController {

    private final UserRepository userRepository;
    private final AnalyticsService analyticsService;
    private final OrderRepository orderRepository;
    private final CompanyRepository companyRepository;
    private final ExcelExportService excelExportService;

    @GetMapping
    public String showAnalytics(Model model, Authentication authentication) {

        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("User not found"));
        Company company = user.getCompany();

        if (company == null) return "redirect:/companies";

        model.addAttribute("currentUser", user);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("company", company);

        boolean isOwner = company.getOwner() != null && company.getOwner().getId().equals(user.getId());
        if (!isOwner && user.getRole() != Role.ANALYST) {
            return "redirect:/dashboard";
        }

        model.addAttribute("totalSpend", analyticsService.getTotalSpend(company));

        if (company.getType() == CompanyType.CUSTOMER) {
            BigDecimal spentThisMonth = analyticsService.getSpentThisMonth(company);
            double budgetUsage = analyticsService.getBudgetUsagePercentage(company);

            model.addAttribute("spentThisMonth", spentThisMonth);
            model.addAttribute("budgetUsage", budgetUsage);
            model.addAttribute("company", company);

            model.addAttribute("monthlyStats", analyticsService.getMonthlyStats(company));
            model.addAttribute("categoryStats", analyticsService.getSpendByCategory(company));
            model.addAttribute("abcData", analyticsService.getABCAnalysis(company));
            model.addAttribute("reliabilityData", analyticsService.getSupplierReliability(company));
            model.addAttribute("prediction", analyticsService.predictNextOrderDate(company));

            Map<String, Long> statusStats = analyticsService.getOrderStatusStats(company);
            model.addAttribute("totalOrders", statusStats.values().stream().mapToLong(Long::longValue).sum());
            model.addAttribute("activeOrders", statusStats.getOrDefault("CREATED", 0L) + statusStats.getOrDefault("CONFIRMED", 0L));

            return "analytics/customer";
        } else {
            model.addAttribute("monthlyStats", analyticsService.getMonthlyStats(company));
            model.addAttribute("topProducts", analyticsService.getTopProducts(company));

            Map<String, Long> statusStats = analyticsService.getOrderStatusStatsForSupplier(company);
            model.addAttribute("totalOrders", statusStats.values().stream().mapToLong(Long::longValue).sum());
            model.addAttribute("activeOrders", statusStats.getOrDefault("CREATED", 0L) + statusStats.getOrDefault("CONFIRMED", 0L));

            return "analytics/supplier";
        }
    }
    @PostMapping("/set-budget")
    public String setBudget(@RequestParam BigDecimal budget, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Company company = user.getCompany();

        if (company.getOwner().getId().equals(user.getId()) || user.getRole() == Role.ANALYST) {
            company.setMonthlyBudget(budget);
            companyRepository.save(company);
        }

        return "redirect:/analytics";
    }


    @GetMapping("/export")
    public ResponseEntity<InputStreamResource> exportToExcel(Authentication authentication) throws IOException {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Company company = user.getCompany();

        List<Order> orders;
        if (company.getType() == CompanyType.CUSTOMER) {
            orders = orderRepository.findByCustomerCompanyOrderByCreatedAtDesc(company);
        } else {
            orders = orderRepository.findBySupplierCompanyOrderByCreatedAtDesc(company);
        }

        ByteArrayInputStream in = excelExportService.exportOrdersToExcel(orders);

        HttpHeaders headers = new HttpHeaders();
        headers.add("Content-Disposition", "attachment; filename=orders_report.xlsx");

        return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(new InputStreamResource(in));
    }

}