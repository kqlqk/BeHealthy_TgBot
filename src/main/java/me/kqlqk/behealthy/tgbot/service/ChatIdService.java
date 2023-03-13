package me.kqlqk.behealthy.tgbot.service;

import me.kqlqk.behealthy.tgbot.model.ChatId;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public interface ChatIdService {
    List<ChatId> getAll();

    void save(ChatId chatId);
}
