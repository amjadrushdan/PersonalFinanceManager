package com.finance.pfm.entity;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "users") // avoid conflict with SQL reserved word 'user'
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String telegramChatId; // link user to Telegram

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Account> accounts;

    // Getters and setters
}
