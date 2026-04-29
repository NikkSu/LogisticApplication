package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.*;
import com.logistics.suppliers.repository.OrderItemRepository;
import com.logistics.suppliers.repository.OrderRepository;
import com.logistics.suppliers.repository.ProductRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.OrderService;
import com.logistics.suppliers.util.PdfService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Controller
@RequestMapping("/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;
    private final OrderItemRepository orderItemRepository;
    private final PdfService pdfService;

    @PostMapping("/create")
    public String placeOrder(Authentication authentication, RedirectAttributes redirectAttributes) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        try {
            orderService.createOrdersFromCart(user);
            redirectAttributes.addFlashAttribute("message", "Заказы успешно сформированы и отправлены поставщикам!");
            redirectAttributes.addFlashAttribute("messageType", "success");
            return "redirect:/orders/my";
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("message", "Ошибка оформления: " + e.getMessage());
            redirectAttributes.addFlashAttribute("messageType", "error");
            return "redirect:/cart";
        }
    }

    @GetMapping("/my")
    public String listMyOrders(Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Company company = user.getCompany();

        if (company == null) return "redirect:/companies";

        List<Order> orders;
        if (company.getType() == CompanyType.SUPPLIER) {
            orders = orderRepository.findBySupplierCompanyOrderByCreatedAtDesc(company);
            model.addAttribute("pageTitle", "Входящие заказы (Продажи)");
        } else {
            orders = orderRepository.findByCustomerCompanyOrderByCreatedAtDesc(company);
            model.addAttribute("pageTitle", "История закупок");
        }

        model.addAttribute("orders", orders);
        model.addAttribute("currentUser", user);
        return "orders-list";
    }
    @GetMapping("/view/{id}")
    public String viewOrder(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new RuntimeException("Пользователь не найден"));

        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        java.math.BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
        model.addAttribute("currentUser", user);

        return "order-view";
    }

    @GetMapping("/download/{id}")
    @ResponseBody
    public ResponseEntity<byte[]> downloadOrderPdf(@PathVariable Long id) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));
        List<OrderItem> items = orderItemRepository.findByOrder(order);

        java.math.BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(java.math.BigDecimal.valueOf(item.getQuantity())))
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);

        Map<String, Object> data = Map.of(
                "order", order,
                "items", items,
                "total", total
        );

        byte[] pdfBytes = pdfService.generatePdfFromHtml("supplier/order-pdf", data);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=order_" + id + ".pdf")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }


    @PostMapping("/{id}/status")
    public String updateOrderStatus(@PathVariable Long id,
                                    @RequestParam OrderStatus newStatus,
                                    Authentication authentication,
                                    RedirectAttributes redirectAttributes) {
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        order.setStatus(newStatus);
        order.setUpdatedAt(LocalDateTime.now());
        if (newStatus == OrderStatus.DELIVERED) {
            order.setDeliveredAt(LocalDateTime.now());
            order.setActualDeliveryDate(LocalDateTime.now());
        }
        orderRepository.save(order);

        redirectAttributes.addFlashAttribute("message", "Статус заказа #" + id + " обновлен на " + newStatus);
        return "redirect:/orders/view/" + id;
    }
}