package com.powerpuff.backend.entity;

import jakarta.persistence.*;
import java.math.BigDecimal;

/** Базовые поля каталога; ID назначается EKT, не нашей БД. */
@Entity
@Table(name = "products")
public class Product {
    @Id
    private Long id;
    @Column(nullable = false, columnDefinition = "text")
    private String name;
    private String article;
    private String barcode;
    @Column(precision = 38, scale = 10)
    private BigDecimal price;
    @Column(precision = 38, scale = 10)
    private BigDecimal quantity;

    protected Product() {}

    public Product(Long id, String name, String article, String barcode,
                   BigDecimal price, BigDecimal quantity) {
        this.id = id;
        this.name = name;
        this.article = article;
        this.barcode = barcode;
        this.price = price;
        this.quantity = quantity;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getArticle() { return article; }
    public String getBarcode() { return barcode; }
    public BigDecimal getPrice() { return price; }
    public BigDecimal getQuantity() { return quantity; }
}
