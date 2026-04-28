package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.Favorite;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import com.logistics.suppliers.repository.FavoriteRepository;
import com.logistics.suppliers.repository.ProductRepository;
import com.logistics.suppliers.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.Optional;

@Controller
@RequestMapping("/favorites")
@RequiredArgsConstructor
public class FavoriteController {

    private final FavoriteRepository favoriteRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    @PostMapping("/toggle/{productId}")
    public String toggleFavorite(@PathVariable Long productId,
                                 Authentication authentication,
                                 HttpServletRequest request) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new RuntimeException("Товар не найден"));

        Optional<Favorite> existing = favoriteRepository.findByUserAndProduct(user, product);

        if (existing.isPresent()) {
            favoriteRepository.delete(existing.get());
        } else {
            favoriteRepository.save(new Favorite(user, product));
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer != null ? referer : "/dashboard");
    }
}