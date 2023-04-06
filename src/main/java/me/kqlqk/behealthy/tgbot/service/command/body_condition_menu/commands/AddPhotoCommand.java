package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.cfg.BotCfg;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.AddUserPhotoDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.FullUserPhotoDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.BodyConditionMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.File;
import org.telegram.telegrambots.meta.api.objects.PhotoSize;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@Scope("prototype")
@Slf4j
public class AddPhotoCommand extends Command {
    private final SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final BotCfg botCfg;

    @Autowired
    public AddPhotoCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, BotCfg botCfg) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.botCfg = botCfg;
        sendMessage = new SendMessage();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage.setText("Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC || tgUser.getCommandSate() == CommandState.RETURN_TO_BODY_CONDITION_MENU) {

            List<FullUserPhotoDTO> fullUserPhotoDTOs;
            try {
                fullUserPhotoDTOs = gatewayClient.getAllUserPhotosAndFiles(tgUser.getUserId(), accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage.setText("Something went wrong");
                return;
            }

            fullUserPhotoDTOs.sort(Comparator.comparing(FullUserPhotoDTO::getPhotoDate));
            Date lastPhotoDate = fullUserPhotoDTOs.get(fullUserPhotoDTOs.size() - 1).getPhotoDate();
            LocalDateTime localDateTime = LocalDateTime.ofInstant(lastPhotoDate.toInstant(), ZoneId.systemDefault());
            if (!LocalDateTime.now().isAfter(localDateTime.plusMonths(1))) {
                sendMessage.setText("Sorry, for more contrast of changes you can add new image one month after the previous");
                return;
            }

            sendMessage.setText("Send photo of your current condition");
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.WAIT_FOR_PHOTO);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.WAIT_FOR_PHOTO) {
            if (!update.getMessage().hasPhoto()) {
                sendMessage.setText("Please send the photo");
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            PhotoSize photoSize = update.getMessage().getPhoto().stream()
                    .max(Comparator.comparing(PhotoSize::getFileSize))
                    .orElse(null);

            GetFile getFile = new GetFile();
            getFile.setFileId(photoSize.getFileId());

            String encodedPhoto;
            try {
                File file = (File) botCfg.executeSth(getFile);

                String fileUrl = "https://api.telegram.org/file/bot" + botCfg.getBotToken() + "/" + file.getFilePath();

                InputStream inputStream = new URL(fileUrl).openStream();

                byte[] content = inputStream.readAllBytes();
                encodedPhoto = Base64.getEncoder().encodeToString(content);

                inputStream.close();
            }
            catch (TelegramApiException | IOException e) {
                sendMessage.setText("Something went wrong");
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

                log.error("Something went wrong", e);
                return;
            }

            DateFormat simpleDateFormat = new SimpleDateFormat("dd-MM-yy");

            AddUserPhotoDTO addUserPhotoDTO = new AddUserPhotoDTO(simpleDateFormat.format(new Date()), encodedPhoto);

            try {
                gatewayClient.saveUserPhoto(tgUser.getUserId(), addUserPhotoDTO, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage.setText("Successfully added");
            sendMessage.setReplyMarkup(BodyConditionMenu.initKeyboard());
        }
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("add next photo");
        res.add("add photo");
        res.add("/add_photo");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
