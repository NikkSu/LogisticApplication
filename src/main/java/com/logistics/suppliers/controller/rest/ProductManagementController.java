package com.logistics.suppliers.controller.rest;

import com.logistics.suppliers.model.Category;
import com.logistics.suppliers.model.CompanyType;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.model.User;
import com.logistics.suppliers.repository.CategoryRepository;
import com.logistics.suppliers.repository.ProductRepository;
import com.logistics.suppliers.repository.UserRepository;
import com.logistics.suppliers.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/my-products")
@RequiredArgsConstructor
public class ProductManagementController {

    private final ProductService productService;
    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    @GetMapping
    public String listMyProducts(Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        if (user.getCompany() == null || user.getCompany().getType() != CompanyType.SUPPLIER) {
            return "redirect:/dashboard";
        }

        model.addAttribute("products", productService.getProductsByCompany(user.getCompany()));
        return "supplier/my-products";
    }

    @GetMapping("/add")
    public String addProductForm(Model model) {
        model.addAttribute("product", new Product());
        model.addAttribute("categories", categoryRepository.findAll());
        return "supplier/product-form";
    }

    @PostMapping("/save")
    public String saveProduct(@ModelAttribute Product product, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        product.setSupplier(user.getCompany());
        productService.saveProduct(product);
        return "redirect:/my-products?success=true";
    }
    @PostMapping("/delete/{id}")
    public String deleteProduct(@PathVariable Long id, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Product product = productService.getProductById(id);

        if (product.getSupplier().getId().equals(user.getCompany().getId())) {
            productRepository.delete(product);
        }

        return "redirect:/my-products?deleted=true";
    }

    // Показать форму для редактирования
    @GetMapping("/edit/{id}")
    public String editProductForm(@PathVariable Long id, Model model, Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName()).get();
        Product product = productService.getProductById(id);

        // Проверка прав (только свой товар)
        if (!product.getSupplier().getId().equals(user.getCompany().getId())) {
            return "redirect:/my-products";
        }

        model.addAttribute("product", product);
        model.addAttribute("categories", categoryRepository.findAll());
        return "supplier/product-form";
    }

    // Новый метод для быстрого создания категории прямо из формы или отдельно
    @PostMapping("/categories/add")
    public String addCategory(@RequestParam String categoryName, RedirectAttributes redirectAttributes) {
        if (categoryName == null || categoryName.trim().isEmpty()) {
            return "redirect:/my-products/add";
        }

        String cleanName = categoryName.trim();
        if (categoryRepository.findByNameIgnoreCase(cleanName).isPresent()) {
            redirectAttributes.addFlashAttribute("error", "Такая категория уже существует");
        } else {
            Category cat = new Category();
            cat.setName(cleanName);
            categoryRepository.save(cat);
            redirectAttributes.addFlashAttribute("message", "Категория добавлена");
        }
        return "redirect:/my-products/add";
    }
}