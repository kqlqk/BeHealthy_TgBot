package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class BackCommand implements Command {
    private final TelegramUserService telegramUserService;

    public BackCommand(TelegramUserService telegramUserService) {
        this.telegramUserService = telegramUserService;
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        if (tgUser.getCommandSate() != CommandState.BASIC) {
            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);
        }
    }

    @Override
    public SendMessage getSendMessage() {
        return null;
    }
}
