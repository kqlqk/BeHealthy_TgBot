package me.kqlqk.behealthy.tgbot.service.command.commands.guest;

import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.UserDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
public class RegistrationCommand implements Command {
    private final GatewayClient gatewayClient;
    private final TelegramUserService telegramUserService;

    private SendMessage sendMessage;

    @Autowired
    public RegistrationCommand(GatewayClient gatewayClient, TelegramUserService telegramUserService) {
        this.gatewayClient = gatewayClient;
        this.telegramUserService = telegramUserService;
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        String userMessage = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        if (tgUser.isActive()) {
            sendMessage = new SendMessage(chatId, "You already signed up");
            sendMessage.setReplyMarkup(defaultKeyboard(true));
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            String text = "Enter your email, name and password." +
                    "\nUse the following pattern: email name password";
            sendMessage = new SendMessage(chatId, text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.REGISTRATION_WAIT_FOR_DATA);
            tgUser.setActive(false);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.REGISTRATION_WAIT_FOR_DATA) {
            String[] data;

            try {
                data = splitData(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            UserDTO userDTO = new UserDTO(data[0], data[1], data[2]);

            TokensDTO tokensDTO;
            try {
                tokensDTO = gatewayClient.createUser(userDTO);
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setRefreshToken(tokensDTO.getRefreshToken());
            tgUser.setUserId(tokensDTO.getUserId());
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(true);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(update.getMessage().getChatId().toString(), "Successfully signed up");
            sendMessage.setReplyMarkup(defaultKeyboard(true));
            return;
        }

        sendMessage = null;
    }

    private String[] splitData(String data) {
        String[] split = data.split(" ");

        if (split.length < 3) {
            throw new BadUserDataException("Please, use the following pattern: email name password");
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/registration");
        res.add("registration");
        res.add("sign up");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
