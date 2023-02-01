package me.kqlqk.behealthy.tgbot.service.impl;

import lombok.NonNull;
import me.kqlqk.behealthy.tgbot.exception.TelegramUserAlreadyExistsException;
import me.kqlqk.behealthy.tgbot.exception.TelegramUserNotFoundException;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.repository.TelegramUserRepository;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class TelegramUserServiceImpl implements TelegramUserService {
    private final TelegramUserRepository telegramUserRepository;

    @Autowired
    public TelegramUserServiceImpl(TelegramUserRepository telegramUserRepository) {
        this.telegramUserRepository = telegramUserRepository;
    }

    @Override
    public TelegramUser getByTelegramId(long telegramId) {
        return telegramUserRepository.getByTelegramId(telegramId);
    }

    @Override
    public boolean existsByTelegramId(long telegramId) {
        return telegramUserRepository.existsByTelegramId(telegramId);
    }

    @Override
    public void save(@NonNull TelegramUser telegramUser) {
        if (existsByTelegramId(telegramUser.getTelegramId())) {
            throw new TelegramUserAlreadyExistsException("TelegramUser with telegramId = " + telegramUser.getTelegramId() + " already exists");
        }

        telegramUserRepository.save(telegramUser);
    }

    @Override
    public void update(@NonNull TelegramUser telegramUser) {
        if (!existsByTelegramId(telegramUser.getTelegramId())) {
            throw new TelegramUserNotFoundException("TelegramUser with telegramId = " + telegramUser.getTelegramId() + " not found");
        }

        telegramUserRepository.save(telegramUser);
    }
}
