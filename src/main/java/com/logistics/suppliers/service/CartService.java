package com.logistics.suppliers.service;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartItemRepository cartItemRepository;

    @Transactional
    public void addToCart(User user, Product product, Integer quantity) {
        Company company = user.getCompany();
        if (company == null) {
            throw new RuntimeException("Вы должны состоять в компании для совершения покупок");
        }

        // Ищем, есть ли уже этот товар в корзине КОМПАНИИ
        Optional<CartItem> existingItem = cartItemRepository.findByCompanyAndProduct(company, product);

        if (existingItem.isPresent()) {
            CartItem item = existingItem.get();
            item.setQuantity(item.getQuantity() + quantity);
            cartItemRepository.save(item);
        } else {
            CartItem newItem = new CartItem();
            newItem.setCompany(company); // Привязка к компании
            newItem.setAddedBy(user);    // Запоминаем, кто добавил
            newItem.setProduct(product);
            newItem.setQuantity(quantity);
            cartItemRepository.save(newItem);
        }
    }

    public List<CartItem> getCartForCompany(Company company) {
        return cartItemRepository.findByCompany(company);
    }

    public BigDecimal calculateTotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProduct().getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}