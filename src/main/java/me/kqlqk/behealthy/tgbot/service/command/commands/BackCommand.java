package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
public class BackCommand implements Command {
    private SendMessage sendMessage;
    private final TelegramUserService telegramUserService;

    public BackCommand(TelegramUserService telegramUserService) {
        this.telegramUserService = telegramUserService;
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        String chatId = update.getMessage().getChatId().toString();

        if (tgUser.getCommandSate() != CommandState.BASIC) {
            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);
        }

        sendMessage = new SendMessage(chatId, "Ok");
        sendMessage.setReplyMarkup(defaultKeyboard(tgUser.isActive()));
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/back");
        res.add("back");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
