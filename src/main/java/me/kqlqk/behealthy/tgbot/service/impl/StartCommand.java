package me.kqlqk.behealthy.tgbot.service.impl;

import me.kqlqk.behealthy.tgbot.service.Command;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Component
public class StartCommand implements Command {
    private SendMessage sendMessage;

    @Override
    public void handle(Update update) {
        String text = "Hello, " + update.getMessage().getChat().getFirstName() + ". You should log in into your account.";

        this.sendMessage = new SendMessage(update.getMessage().getChatId().toString(), text);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
