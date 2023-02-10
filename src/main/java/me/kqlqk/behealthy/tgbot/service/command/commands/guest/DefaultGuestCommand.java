package me.kqlqk.behealthy.tgbot.service.command.commands.guest;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class DefaultGuestCommand implements Command {
    private SendMessage sendMessage;

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        sendMessage = new SendMessage(update.getMessage().getChatId().toString(), "Please, sign in or sign up");
        sendMessage.setReplyMarkup(defaultKeyboard(false));
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
