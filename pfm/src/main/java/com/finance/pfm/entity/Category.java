package com.finance.pfm.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Category {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;    // e.g., Food, Transport, Salary
    private String type;    // INCOME / EXPENSE

    @OneToMany(mappedBy = "category")
    private List<Transaction> transactions;

    // Getters and setters
}
