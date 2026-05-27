package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.CartItem;
import com.logistics.suppliers.model.Company;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import com.logistics.suppliers.repository.CartItemRepository;
import com.logistics.suppliers.repository.CompanyRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.AnalyticsService;
import com.logistics.suppliers.service.CartService;
import com.logistics.suppliers.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/cart")
@RequiredArgsConstructor
public class CartController {
    private final CartService cartService;
    private final ProductService productService;
    private final UserRepository userRepository;
    private final CartItemRepository cartItemRepository;
    private final AnalyticsService analyticsService;

    @GetMapping
    public String viewCart(Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Company company = user.getCompany();
        if (company == null) return "redirect:/companies";

        List<CartItem> items = cartService.getCartForCompany(company);
        BigDecimal cartTotal = cartService.calculateTotal(items);

        BigDecimal monthlyLimit = company.getMonthlyBudget() != null ? company.getMonthlyBudget() : BigDecimal.ZERO;
        BigDecimal spentThisMonth = analyticsService.getSpentThisMonth(company);
        BigDecimal totalAfterOrder = spentThisMonth.add(cartTotal);

        BigDecimal excess = BigDecimal.ZERO;
        if (monthlyLimit.compareTo(BigDecimal.ZERO) > 0 && totalAfterOrder.compareTo(monthlyLimit) > 0) {
            excess = totalAfterOrder.subtract(monthlyLimit);
        }

        Map<Company, List<CartItem>> groupedItems = items.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSupplier()));

        model.addAttribute("groupedItems", groupedItems);
        model.addAttribute("items", items);
        model.addAttribute("total", cartTotal);
        model.addAttribute("monthlyLimit", monthlyLimit);
        model.addAttribute("spentThisMonth", spentThisMonth);
        model.addAttribute("isOverBudget", excess.compareTo(BigDecimal.ZERO) > 0);
        model.addAttribute("excessAmount", excess);

        return "cart";
    }

    @PostMapping("/add/{productId}")
    public String addToCart(@PathVariable Long productId,
                            @RequestParam(defaultValue = "1") Integer quantity,
                            Authentication authentication,
                            RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        try {
            Product product = productService.getProductById(productId);
            cartService.addToCart(user, product, quantity);
            redirectAttributes.addFlashAttribute("message", "Товар добавлен в корзину компании");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/dashboard";
    }

    @PostMapping("/remove/{id}")
    public String removeFromCart(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Позиция не найдена"));

        if (item.getCompany().getId().equals(user.getCompany().getId())) {
            cartItemRepository.delete(item);
        }

        return "redirect:/cart";
    }

    @PostMapping("/update/{id}")
    public String updateQuantity(@PathVariable Long id,
                                 @RequestParam Integer quantity,
                                 Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        CartItem item = cartItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Позиция не найдена"));

        if (item.getCompany().getId().equals(user.getCompany().getId())) {
            if (quantity > 0) {
                item.setQuantity(quantity);
                cartItemRepository.save(item);
            } else {
                cartItemRepository.delete(item);
            }
        }
        return "redirect:/cart";
    }
}