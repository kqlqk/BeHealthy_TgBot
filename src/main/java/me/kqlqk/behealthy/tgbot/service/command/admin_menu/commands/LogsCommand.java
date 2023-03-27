package me.kqlqk.behealthy.tgbot.service.command.admin_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class LogsCommand extends Command {
    @Value("${logs.dir}")
    private String BASE_DIRECTORY;

    private SendMessage sendMessage;
    private final List<Object> sendObjects;
    private final TelegramUserService telegramUserService;

    @Autowired
    public LogsCommand(TelegramUserService telegramUserService) {
        this.telegramUserService = telegramUserService;
        sendObjects = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage = new SendMessage(chatId, "Choose service");
            sendMessage.setReplyMarkup(initKeyboard());

            tgUser.setCommandSate(CommandState.LOGS_WAIT_FOR_CHOOSING);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.LOGS_WAIT_FOR_CHOOSING) {
            StringBuilder output = new StringBuilder();
            String path;

            if (userMessage.equalsIgnoreCase("Authentication service")) {
                path = BASE_DIRECTORY + "/authenticationService-logs.log";
            }
            else if (userMessage.equalsIgnoreCase("User condition service")) {
                path = BASE_DIRECTORY + "/userConditionService-logs.log";
            }
            else if (userMessage.equalsIgnoreCase("Workout service")) {
                path = BASE_DIRECTORY + "/workoutService-logs.log";
            }
            else if (userMessage.equalsIgnoreCase("Gateway")) {
                path = BASE_DIRECTORY + "/gateway-logs.log";
            }
            else if (userMessage.equalsIgnoreCase("TgBot")) {
                path = BASE_DIRECTORY + "/tgBot-logs.log";
            }
            else {
                sendMessage = new SendMessage(chatId, "Nothing found");
                sendMessage.setReplyMarkup(initKeyboard());
                return;
            }

            try (FileReader reader = new FileReader(path)) {
                int i;
                while ((i = reader.read()) != -1) {
                    output.append((char) i);
                }
            }
            catch (IOException e) {
                this.sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(initKeyboard());
                return;
            }

            for (int i = 0; i < output.length(); i += 4000) {
                int endIndex = Math.min(output.length(), i + 4000);
                String substring = output.substring(i, endIndex);

                SendMessage sendMessage = new SendMessage(chatId, substring);
                sendMessage.setReplyMarkup(initKeyboard());
                sendObjects.add(sendMessage);
            }

            if (sendObjects.size() > 5) {
                sendObjects.subList(0, sendObjects.size() - 5).clear();
            }
        }

    }

    private static ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Authentication service");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("User condition service");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Workout service");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Gateway");
        keyboardRow.add("TgBot");
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
        res.add("logs");
        res.add("/log");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }

    @Override
    public Object[] getSendObjects() {
        return sendObjects.toArray(new Object[sendObjects.size()]);
    }
}
