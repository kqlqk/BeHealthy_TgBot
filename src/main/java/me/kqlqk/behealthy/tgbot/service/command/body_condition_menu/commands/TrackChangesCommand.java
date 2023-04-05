package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.FullUserPhotoDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.objects.InputFile;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.io.File;
import java.io.IOException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Scope("prototype")
@Slf4j
public class TrackChangesCommand extends Command {
    @Value("${files.tmp.dir}")
    private String dir;

    private SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public TrackChangesCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        sendObjects = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        tgUser.setCommandSate(CommandState.RETURN_TO_BODY_CONDITION_MENU);
        telegramUserService.update(tgUser);

        List<FullUserPhotoDTO> fullUserPhotoDTOs;
        try {
            fullUserPhotoDTOs = gatewayClient.getAllUserPhotosAndFiles(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            if (!e.getMessage().equalsIgnoreCase("Photos not found")) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                log.error("Something went wrong", e);
                return;
            }

            sendMessage = new SendMessage(chatId, "Add your first photo");
            sendMessage.setReplyMarkup(addFirstPhoto());
            return;
        }

        fullUserPhotoDTOs.sort(Comparator.comparing(FullUserPhotoDTO::getPhotoDate));

        FullUserPhotoDTO photo = fullUserPhotoDTOs.get(fullUserPhotoDTOs.size() - 1);

        byte[] decodedBytes = Base64.getDecoder().decode(photo.getEncodedPhoto());

        String[] filePath = photo.getPhotoPath().split("/");
        String fileName = filePath[filePath.length - 1];

        File file;
        try {
            file = new File(dir + "/" + fileName);
            FileUtils.writeByteArrayToFile(file, decodedBytes);
            file.createNewFile();
        }
        catch (IOException e) {
            sendMessage = new SendMessage(chatId, "Something went wrong");
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            log.error("Something went wrong", e);
            return;
        }

        DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy");

        SendPhoto sendPhoto = new SendPhoto(chatId, new InputFile(file));
        sendPhoto.setCaption("This photo was taken " + dateFormat.format(photo.getPhotoDate()));
        sendPhoto.setReplyMarkup(initKeyboard(Optional.of(photo.getPhotoDate())));

        sendObjects.add(sendPhoto);
    }

    private static ReplyKeyboardMarkup addFirstPhoto() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Add photo");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    public static ReplyKeyboardMarkup initKeyboard(Optional<Date> lastPhotoDate) {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();

        if (lastPhotoDate.isPresent()) {
            LocalDateTime localDateTime = LocalDateTime.ofInstant(lastPhotoDate.get().toInstant(), ZoneId.systemDefault());

            if (LocalDateTime.now().isAfter(localDateTime.plusMonths(1))) {
                keyboardRow.add("Add next photo");
                keyboardRows.add(keyboardRow);
            }
        }

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Load old photos");
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
        res.add("track my changes \uD83D\uDCC8");
        res.add("track my changes");
        res.add("/track_my_changes");

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
