package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
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

        sendMessage = new SendMessage(chatId, "Ok");

        switch (tgUser.getCommandSate()) {
            case ADD_FOOD_WAIT_FOR_DATA:
            case CHANGE_KCALS_GOAL_WAIT_FOR_CHOOSING:
            case CHANGE_KCALS_GOAL_WAIT_FOR_DATA:
                sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());
                break;

            default:
                sendMessage.setReplyMarkup(defaultKeyboard(tgUser.isActive()));
                break;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            return;
        }

        tgUser.setCommandSate(CommandState.BASIC);
        telegramUserService.update(tgUser);
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/back");
        res.add("back ↩");
        res.add("back");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
