package me.kqlqk.behealthy.tgbot.service;

import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

public interface Command {
    void handle(Update update);

    SendMessage getSendMessage();
}
