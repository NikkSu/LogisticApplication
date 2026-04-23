package com.logistics.suppliers.controller.common;

import com.logistics.suppliers.dto.AuthResponse;
import com.logistics.suppliers.dto.LoginRequest;
import com.logistics.suppliers.dto.RegisterRequest;
import com.logistics.suppliers.model.CompanyType;
import com.logistics.suppliers.model.PriceHistory;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import com.logistics.suppliers.repository.PriceHistoryRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.AuthService;
import com.logistics.suppliers.service.CompanyService;
import com.logistics.suppliers.service.ProductService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final ProductService productService;
    private final AuthService authService;
    private final PriceHistoryRepository priceHistoryRepository;

    @GetMapping("/register")
    public String registerForm(Model model) {
        if (!model.containsAttribute("registerRequest")) {
            model.addAttribute("registerRequest", new RegisterRequest());
        }
        return "register";
    }

    @PostMapping("/register-submit")
    public String registerSubmit(@Valid @ModelAttribute("registerRequest") RegisterRequest request,
                                 BindingResult result,
                                 Model model) {
        if (result.hasErrors()) {
            return "register";
        }

        try {
            authService.register(request);
            return "redirect:/login?success";
        } catch (Exception e) {
            model.addAttribute("error", e.getMessage());
            return "register";
        }
    }

    @GetMapping("/product/{id}")
    public String productDetail(@PathVariable Long id, Model model) {
        Product product = productService.getProductById(id);
        List<Product> similar = productService.getSimilarProducts(product);

        List<PriceHistory> history = priceHistoryRepository.findByProductIdOrderByChangedAtAsc(id);

        List<String> historyLabels = history.stream()
                .map(h -> h.getChangedAt().getDayOfMonth() + " " + getMonthName(h.getChangedAt().getMonthValue()))
                .toList();

        List<java.math.BigDecimal> historyPrices = history.stream()
                .map(PriceHistory::getPrice)
                .toList();

        model.addAttribute("product", product);
        model.addAttribute("similarProducts", similar);
        model.addAttribute("historyLabels", historyLabels);
        model.addAttribute("historyPrices", historyPrices);

        return "product-detail";
    }

    private String getMonthName(int month) {
        String[] months = {"янв", "фев", "мар", "апр", "май", "июн", "июл", "авг", "сен", "окт", "ноя", "дек"};
        return months[month - 1];
    }

    @GetMapping("/login")
    public String loginForm(Model model) {
        if (!model.containsAttribute("loginRequest")) {
            model.addAttribute("loginRequest", new LoginRequest());
        }
        return "login";
    }

    @PostMapping("/login-submit")
    public String loginSubmit(@ModelAttribute LoginRequest loginRequest,
                              Model model,
                              HttpSession session) {
        try {
            AuthResponse authResponse = authService.login(loginRequest);
            session.setAttribute("jwt", authResponse.getToken());
            return "redirect:/dashboard";
        } catch (RuntimeException e) {
            model.addAttribute("error", "Неверный логин или пароль");
            return "login";
        }
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model,
                            @RequestParam(required = false) String search,
                            @RequestParam(required = false) Long category,
                            @RequestParam(defaultValue = "0") int page,
                            Authentication authentication) {

        User currentUser = userRepository.findByEmail(authentication.getName()).get();
        model.addAttribute("currentUser", currentUser); // Теперь HTML видит компанию юзера

        Page<Product> productPage = productService.getFilteredProducts(search, category, page, 25);

        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("userEmail", currentUser.getEmail());

        return "dashboard";
    }

}