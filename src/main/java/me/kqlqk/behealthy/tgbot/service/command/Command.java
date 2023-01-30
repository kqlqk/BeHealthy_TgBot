package me.kqlqk.behealthy.tgbot.service.command;

import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    default void handle(Update update, TelegramUser tgUser) {

    }

    default void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO) {

    }

    SendMessage getSendMessage();
}
