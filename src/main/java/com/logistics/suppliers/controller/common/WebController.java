package com.logistics.suppliers.controller.common;

import com.logistics.suppliers.dto.AuthResponse;
import com.logistics.suppliers.dto.LoginRequest;
import com.logistics.suppliers.dto.RegisterRequest;
import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.FavoriteRepository;
import com.logistics.suppliers.repository.PriceHistoryRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.AuthService;
import com.logistics.suppliers.service.CartService;
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
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequiredArgsConstructor
public class WebController {

    private final UserRepository userRepository;
    private final CompanyService companyService;
    private final ProductService productService;
    private final AuthService authService;
    private final PriceHistoryRepository priceHistoryRepository;
    private final CartService cartService;
    private final FavoriteRepository favoriteRepository;

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
    public String productDetail(@PathVariable Long id, Model model, Authentication auth) {
        Product product = productService.getProductById(id);
        List<Product> similar = productService.getSimilarProducts(product);

        User user = userRepository.findByEmail(auth.getName()).get();
        model.addAttribute("currentUser", user);
        model.addAttribute("userEmail", user.getEmail());
        model.addAttribute("favoriteIds", favoriteRepository.findByUser(user).stream().map(f -> f.getProduct().getId()).toList());
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
        model.addAttribute("currentUser", currentUser);
        Company company = currentUser.getCompany();

        boolean isOwner = company != null && company.getOwner() != null &&
                company.getOwner().getId().equals(currentUser.getId());

        if (currentUser.getRole() == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }

        if (currentUser.getCompany() != null && currentUser.getCompany().getType() == CompanyType.CUSTOMER) {
            List<CartItem> cartItems = cartService.getCartForCompany(currentUser.getCompany());
            int cartSize = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
            model.addAttribute("cartSize", cartSize);
        }

        Page<Product> productPage = productService.getFilteredProducts(search, category, page, 25);

        List<Long> favoriteProductIds = favoriteRepository.findByUser(currentUser)
                .stream().map(f -> f.getProduct().getId()).toList();
        model.addAttribute("isOwner", isOwner);
        model.addAttribute("currentUser", currentUser);
        model.addAttribute("favoriteIds", favoriteProductIds);
        model.addAttribute("products", productPage.getContent());
        model.addAttribute("totalPages", productPage.getTotalPages());
        model.addAttribute("currentPage", page);
        model.addAttribute("categories", productService.getAllCategories());
        model.addAttribute("searchQuery", search);
        model.addAttribute("selectedCategory", category);
        model.addAttribute("userEmail", currentUser.getEmail());

        return "dashboard";
    }

    @GetMapping("/forgot-password")
    public String forgotPasswordForm() {
        return "auth/forgot-password";
    }

    @PostMapping("/forgot-password")
    public String processForgotPassword(@RequestParam String email, RedirectAttributes redirectAttributes) {
        try {
            authService.sendTemporaryPassword(email.trim());

            redirectAttributes.addFlashAttribute("message", "Временный пароль отправлен!");
            redirectAttributes.addFlashAttribute("messageType", "success");
        } catch (Exception e) {
            System.out.println("Ошибка поиска почты: [" + email + "]");
            redirectAttributes.addFlashAttribute("message", "Ошибка: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
        }
        return "redirect:/forgot-password";
    }

}