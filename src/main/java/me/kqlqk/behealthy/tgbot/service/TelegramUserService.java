package me.kqlqk.behealthy.tgbot.service;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import org.springframework.stereotype.Service;

@Service
public interface TelegramUserService {
    TelegramUser getByTelegramId(long telegramId);

    boolean existsByTelegramId(long telegramId);

    void save(TelegramUser telegramUser);

    void update(TelegramUser telegramUser);
}
