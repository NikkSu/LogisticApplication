package com.logistics.suppliers.controller.common;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.OrderRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

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
}