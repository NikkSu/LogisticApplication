package com.logistics.suppliers.service;

import com.logistics.suppliers.model.Category;
import com.logistics.suppliers.model.Product;
import com.logistics.suppliers.repository.CategoryRepository;
import com.logistics.suppliers.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public List<Product> getSimilarProducts(Product product) {
        if (product.getCategory() == null) return List.of();

        Pageable limitFour = PageRequest.of(0, 4);
        return productRepository.findByCategoryIdAndIdNot(
                product.getCategory().getId(),
                product.getId(),
                limitFour
        ).getContent();
    }

    public List<Category> getAllCategories() {
        return categoryRepository.findAll();
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Товар не найден"));
    }
    public Page<Product> getFilteredProducts(String search, Long categoryId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        String query = (search == null) ? "" : search;

        if (categoryId != null) {
            return productRepository.findByNameContainingIgnoreCaseAndCategoryId(query, categoryId, pageable);
        } else {
            return productRepository.findByNameContainingIgnoreCase(query, pageable);
        }
    }
}