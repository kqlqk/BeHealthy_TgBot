package me.kqlqk.behealthy.tgbot.service.impl;

import me.kqlqk.behealthy.tgbot.exception.ChatIdAlreadyExistsException;
import me.kqlqk.behealthy.tgbot.model.ChatId;
import me.kqlqk.behealthy.tgbot.repository.ChatIdRepository;
import me.kqlqk.behealthy.tgbot.service.ChatIdService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ChatIdServiceImpl implements ChatIdService {
    private final ChatIdRepository chatIdRepository;

    @Autowired
    public ChatIdServiceImpl(ChatIdRepository chatIdRepository) {
        this.chatIdRepository = chatIdRepository;
    }

    @Override
    public List<ChatId> getAll() {
        return chatIdRepository.findAll();
    }

    @Override
    public void save(ChatId chatId) {
        if (chatIdRepository.existsByChatId(chatId.getChatId())) {
            throw new ChatIdAlreadyExistsException("ChatId with chatId = " + chatId.getChatId() + " already exists");
        }

        chatId.setId(0);
        chatIdRepository.save(chatId);
    }
}
