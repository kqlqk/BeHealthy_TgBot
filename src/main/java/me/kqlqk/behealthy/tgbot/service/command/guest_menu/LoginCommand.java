package me.kqlqk.behealthy.tgbot.service.command.guest_menu;

import me.kqlqk.behealthy.tgbot.dto.auth_service.LoginDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
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

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class LoginCommand implements Command {
    private SendMessage sendMessage;
    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public LoginCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        String userMessage = update.getMessage().getText();
        String chatId = update.getMessage().getChatId().toString();

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            String text = "Enter your email and password.\n Use the following pattern: email password";
            sendMessage = new SendMessage(chatId, text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.LOGIN_WAIT_FOR_DATA);
            tgUser.setActive(false);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.LOGIN_WAIT_FOR_DATA) {
            String[] credentials;

            try {
                credentials = splitCredentials(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            LoginDTO loginDTO = new LoginDTO(credentials[0], credentials[1]);

            TokensDTO tokensDTO;
            try {
                tokensDTO = gatewayClient.logInUser(loginDTO);
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

            sendMessage = new SendMessage(chatId, "Successfully signed in");
            sendMessage.setReplyMarkup(defaultKeyboard(true));
        }
    }

    private String[] splitCredentials(String credentials) {
        String[] split = credentials.split(" ");

        if (split.length < 2) {
            throw new BadUserDataException("Please, use the following pattern: email password");
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/login");
        res.add("login");
        res.add("sign in");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
