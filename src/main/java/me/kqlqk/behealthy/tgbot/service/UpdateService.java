package me.kqlqk.behealthy.tgbot.service;

import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public interface UpdateService {
    Object handle(Update update);
}
