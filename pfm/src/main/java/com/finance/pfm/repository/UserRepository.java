package com.finance.pfm.repository;

import com.finance.pfm.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByTelegramChatId(String telegramChatId);
}
