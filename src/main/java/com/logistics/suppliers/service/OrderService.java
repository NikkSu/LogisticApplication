package com.logistics.suppliers.service;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final CartItemRepository cartItemRepository;

    @Transactional
    public void createOrdersFromCart(User user) {
        Company customerCompany = user.getCompany();
        List<CartItem> cartItems = cartItemRepository.findByCompany(customerCompany);

        if (cartItems.isEmpty()) {
            throw new RuntimeException("Корзина пуста");
        }

        Map<Company, List<CartItem>> itemsBySupplier = cartItems.stream()
                .collect(Collectors.groupingBy(item -> item.getProduct().getSupplier()));

        for (Map.Entry<Company, List<CartItem>> entry : itemsBySupplier.entrySet()) {
            Company supplier = entry.getKey();
            List<CartItem> itemsForThisSupplier = entry.getValue();

            Order order = new Order();
            order.setCustomerCompany(customerCompany);
            order.setSupplierCompany(supplier);
            order.setStatus(OrderStatus.CREATED);
            order.setCreatedAt(LocalDateTime.now());


            Order savedOrder = orderRepository.save(order);

            for (CartItem cartItem : itemsForThisSupplier) {
                OrderItem orderItem = new OrderItem();
                orderItem.setOrder(savedOrder);
                orderItem.setProduct(cartItem.getProduct());
                orderItem.setQuantity(cartItem.getQuantity());
                orderItem.setPrice(cartItem.getProduct().getPrice());
                
                orderItemRepository.save(orderItem);
            }
        }

        cartItemRepository.deleteByCompany(customerCompany);
    }
}