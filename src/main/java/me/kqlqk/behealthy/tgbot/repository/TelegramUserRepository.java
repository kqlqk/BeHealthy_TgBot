package me.kqlqk.behealthy.tgbot.repository;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    Optional<TelegramUser> findByTelegramId(long telegramId);

    boolean existsByTelegramId(long telegramId);
}
