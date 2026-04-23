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
        Order order = orderRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Заказ не найден"));

        List<OrderItem> items = orderItemRepository.findByOrder(order);

        BigDecimal total = items.stream()
                .map(item -> item.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        model.addAttribute("order", order);
        model.addAttribute("items", items);
        model.addAttribute("total", total);
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

}