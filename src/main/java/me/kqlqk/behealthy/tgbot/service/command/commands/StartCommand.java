package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class StartCommand implements Command {
    private SendMessage sendMessage;

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        String text;

        if (tgUser.isActive()) {
            text = "You already signed into your account";
        }
        else {
            text = "Hello, " + update.getMessage().getChat().getFirstName() + ". You should sign into your account.";
        }

        this.sendMessage = new SendMessage(update.getMessage().getChatId().toString(), text);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
