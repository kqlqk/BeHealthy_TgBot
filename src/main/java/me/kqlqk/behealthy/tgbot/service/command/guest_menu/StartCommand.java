package me.kqlqk.behealthy.tgbot.service.command.guest_menu;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class StartCommand extends Command {
    private final SendMessage sendMessage;

    public StartCommand() {
        sendMessage = new SendMessage();
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        String text;

        if (tgUser.isActive()) {
            text = "You already signed into your account";
        }
        else {
            text = "Hello, " + update.getMessage().getChat().getFirstName() + ". You should sign in or sign up to your account.";
        }

        sendMessage.setText(text);
        sendMessage.setReplyMarkup(defaultKeyboard(tgUser.isActive()));
    }


    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/start");
        res.add("start");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
