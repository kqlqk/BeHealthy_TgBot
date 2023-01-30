package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class RegistrationCommand implements Command {

    @Override
    public void handle(Update update, TelegramUser tgUser) {

    }

    @Override
    public SendMessage getSendMessage() {
        return null;
    }
}
