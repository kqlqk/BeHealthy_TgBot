package me.kqlqk.behealthy.tgbot.service.command;

import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

public abstract class Command {
    public void handle(Update update, TelegramUser tgUser) {
    }

    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
    }

    public final ReplyKeyboardMarkup defaultKeyboard(boolean userActive) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();

        if (!userActive) {
            keyboardRow.add("Sign in");
            keyboardRow.add("Sign up");
            keyboardRows.add(keyboardRow);
        }
        else {
            keyboardRow.add("Kilocalories tracker \uD83D\uDC40");
            keyboardRows.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add("Workout service \uD83D\uDCAA");
            keyboardRows.add(keyboardRow);

            keyboardRow = new KeyboardRow();
            keyboardRow.add("My body \uD83D\uDE0E");
            keyboardRows.add(keyboardRow);
        }

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    public final ReplyKeyboardMarkup onlyBackCommandKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();

        keyboardRow.add("Back ↩");

        keyboardRows.add(keyboardRow);
        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);

        return keyboard;
    }

    public SendMessage getSendMessage() {
        return null;
    }

    public Object[] getSendObjects() {
        return null;
    }
}
