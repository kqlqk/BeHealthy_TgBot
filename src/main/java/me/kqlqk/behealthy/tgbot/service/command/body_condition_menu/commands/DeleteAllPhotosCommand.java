package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class DeleteAllPhotosCommand extends Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final TrackChangesCommand trackChangesCommand;
    private List<Object> sendObjects;

    @Autowired
    public DeleteAllPhotosCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, TrackChangesCommand trackChangesCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.trackChangesCommand = trackChangesCommand;
        sendMessage = new SendMessage();
        sendObjects = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        String userMessage = update.getMessage().getText();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage.setText("Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC || tgUser.getCommandSate() == CommandState.RETURN_TO_BODY_CONDITION_MENU) {
            sendMessage.setText("Are you sure?");
            sendMessage.setReplyMarkup(initKeyboard());

            tgUser.setCommandSate(CommandState.DELETE_ALL_PHOTOS_WAIT_FOD_CHOOSING);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.DELETE_ALL_PHOTOS_WAIT_FOD_CHOOSING) {
            if (userMessage.equalsIgnoreCase("Yeah, I'm sure")) {
                try {
                    gatewayClient.deleteUserPhoto(tgUser.getUserId(), null, accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    sendMessage.setText("Something went wrong");
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

                    log.error("Something went wrong", e);
                    return;
                }
            }

            trackChangesCommand.handle(update, tgUser, accessTokenDTO, securityState);
            sendObjects = List.of(trackChangesCommand.getSendObjects());
            if (sendObjects.isEmpty()) {
                sendMessage = trackChangesCommand.getSendMessage();
            }
        }
    }

    private static ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Yeah, I'm sure");
        keyboardRow.add("No, that was a mistake");
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
        res.add("delete all my photos");
        res.add("/delete_photos");

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
