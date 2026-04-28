package com.logistics.suppliers.service;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.OrderItemRepository;
import com.logistics.suppliers.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;

    public BigDecimal getTotalSpend(Company company) {
        List<Order> orders = getDeliveredOrders(company);
        return orders.stream()
                .map(Order::getTotalPrice)
                .filter(price -> price != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public Map<String, BigDecimal> getMonthlyStats(Company company) {
        List<Order> orders = getDeliveredOrders(company);

        return orders.stream()
                .sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()))
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().getMonth()
                                .getDisplayName(TextStyle.FULL, new Locale("ru")),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO,
                                order -> order.getTotalPrice() != null ? order.getTotalPrice() : BigDecimal.ZERO, // Защита
                                BigDecimal::add)
                ));
    }

    public Map<String, BigDecimal> getSpendByCategory(Company company) {
        List<Order> orders = getDeliveredOrders(company);

        return orders.stream()
                .flatMap(order -> orderItemRepository.findByOrder(order).stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getCategory().getName(),
                        Collectors.reducing(BigDecimal.ZERO,
                                item -> {
                                    if (item.getPrice() == null || item.getQuantity() == null) return BigDecimal.ZERO;
                                    return item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
                                },
                                BigDecimal::add)
                ));
    }

    public String predictNextOrderDate(Company company) {
        List<Order> orders = getDeliveredOrders(company);

        if (orders.size() < 2) {
            return "Нужно больше данных";
        }

        LocalDateTime lastOrderDate = orders.get(orders.size() - 1).getCreatedAt();
        return lastOrderDate.plusDays(14).format(DateTimeFormatter.ofPattern("dd MMMM yyyy", new Locale("ru")));
    }

    private List<Order> getDeliveredOrders(Company company) {
        if (company.getType() == CompanyType.CUSTOMER) {
            return orderRepository.findByCustomerCompanyAndStatus(company, OrderStatus.DELIVERED);
        } else {
            return orderRepository.findBySupplierCompanyAndStatus(company, OrderStatus.DELIVERED);
        }
    }

    public Map<String, BigDecimal> getABCAnalysis(Company company) {
        List<Order> orders = getDeliveredOrders(company);
        return orders.stream()
                .flatMap(order -> orderItemRepository.findByOrder(order).stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.reducing(BigDecimal.ZERO, OrderItem::getRowTotal, BigDecimal::add)
                ));
    }

    public Map<String, Double> getSupplierReliability(Company company) {
        List<Order> orders = getDeliveredOrders(company);
        return orders.stream()
                .filter(o -> o.getDeliveredAt() != null && o.getCreatedAt() != null)
                .collect(Collectors.groupingBy(
                        o -> o.getSupplierCompany().getName(),
                        Collectors.averagingLong(o ->
                                java.time.Duration.between(o.getCreatedAt(), o.getDeliveredAt()).toDays())
                ));
    }

    public Map<String, Long> getOrderStatusStats(Company company) {
        return orderRepository.findAllByCustomerCompany(company).stream()
                .collect(Collectors.groupingBy(o -> o.getStatus().name(), Collectors.counting()));
    }
    public Map<String, BigDecimal> getSupplierMonthlySales(Company company) {
        List<Order> orders = orderRepository.findBySupplierCompanyAndStatus(company, OrderStatus.DELIVERED);
        return orders.stream()
                .sorted((o1, o2) -> o1.getCreatedAt().compareTo(o2.getCreatedAt()))
                .collect(Collectors.groupingBy(
                        order -> order.getCreatedAt().getMonth().getDisplayName(TextStyle.FULL, new Locale("ru")),
                        LinkedHashMap::new,
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalPrice, BigDecimal::add)
                ));
    }

    public Map<String, BigDecimal> getTopCustomers(Company company) {
        List<Order> orders = orderRepository.findBySupplierCompanyAndStatus(company, OrderStatus.DELIVERED);
        return orders.stream()
                .collect(Collectors.groupingBy(
                        order -> order.getCustomerCompany().getName(),
                        Collectors.reducing(BigDecimal.ZERO, Order::getTotalPrice, BigDecimal::add)
                ));
    }

    public Map<String, Long> getTopSellingProducts(Company company) {
        List<Order> orders = orderRepository.findBySupplierCompanyAndStatus(company, OrderStatus.DELIVERED);
        return orders.stream()
                .flatMap(order -> orderItemRepository.findByOrder(order).stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));
    }

    public Map<String, Long> getOrderStatusStatsForSupplier(Company company) {
        List<Order> orders = orderRepository.findBySupplierCompany(company);
        if (orders == null) return Map.of();

        return orders.stream()
                .filter(o -> o.getStatus() != null)
                .collect(Collectors.groupingBy(
                        order -> order.getStatus().name(),
                        Collectors.counting()
                ));
    }

    public Map<String, Long> getTopProducts(Company company) {
        List<Order> orders = orderRepository.findBySupplierCompanyAndStatus(company, OrderStatus.DELIVERED);

        return orders.stream()
                .flatMap(order -> orderItemRepository.findByOrder(order).stream())
                .collect(Collectors.groupingBy(
                        item -> item.getProduct().getName(),
                        Collectors.summingLong(OrderItem::getQuantity)
                ));
    }

}