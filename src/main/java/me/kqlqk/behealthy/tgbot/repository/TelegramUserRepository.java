package me.kqlqk.behealthy.tgbot.repository;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TelegramUserRepository extends JpaRepository<TelegramUser, Long> {
    TelegramUser getByTelegramId(long telegramId);

    boolean existsByTelegramId(long telegramId);
}
