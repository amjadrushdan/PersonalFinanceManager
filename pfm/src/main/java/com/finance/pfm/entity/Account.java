package com.finance.pfm.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
public class Account {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;   // e.g., Wallet, Bank, Credit Card
    private Double balance = 0.0;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    // private List<Transaction> transactions;

    // Getters and setters
}
