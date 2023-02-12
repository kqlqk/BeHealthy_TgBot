package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands.GetFoodCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
public class KcalsTrackerMenu implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GetFoodCommand getFoodCommand;

    @Autowired
    public KcalsTrackerMenu(TelegramUserService telegramUserService, GetFoodCommand getFoodCommand) {
        this.telegramUserService = telegramUserService;
        this.getFoodCommand = getFoodCommand;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        getFoodCommand.handle(update, tgUser, tokensDTO, securityState);
        sendMessage = getFoodCommand.getSendMessage();
        if (sendMessage.getReplyMarkup() == null) {
            sendMessage.setReplyMarkup(initKeyboard());
        }
    }

    public static ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Add food ➕");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Get today's food \uD83D\uDD3D");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Change kilocalories goal ⚙");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("kilocalories tracker \uD83D\uDC40");
        res.add("kilocalories tracker");
        res.add("kilocalories menu");

        return res;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
