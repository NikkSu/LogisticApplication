package com.logistics.suppliers.model;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PriceHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal price;
    private LocalDateTime changedAt;

    public PriceHistory(Product product, BigDecimal price) {
        this.product = product;
        this.price = price;
        this.changedAt = LocalDateTime.now();
    }
}