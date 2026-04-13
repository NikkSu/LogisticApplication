package com.logistics.suppliers.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    private Category category;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer stockQuantity;

    @NotBlank(message = "Название товара обязательно")
    private String name;

    @NotNull(message = "Цена должна быть указана")
    @Positive(message = "Цена должна быть больше нуля")
    private BigDecimal price;

    private String imageUrl;

    @ManyToOne(optional = false)
    @JoinColumn(name = "supplier_id")
    @NotNull
    private Company supplier;

    private String sku;
}