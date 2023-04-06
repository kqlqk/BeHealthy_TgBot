package me.kqlqk.behealthy.tgbot.service.command.guest_menu;

import me.kqlqk.behealthy.tgbot.dto.auth_service.RegistrationDTO;
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
public class RegistrationCommand extends Command {
    private final SendMessage sendMessage;

    private final GatewayClient gatewayClient;
    private final TelegramUserService telegramUserService;

    @Autowired
    public RegistrationCommand(GatewayClient gatewayClient, TelegramUserService telegramUserService) {
        this.gatewayClient = gatewayClient;
        this.telegramUserService = telegramUserService;
        sendMessage = new SendMessage();
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());
        String userMessage = update.getMessage().getText();

        if (tgUser.isActive()) {
            sendMessage.setText("You already signed up");
            sendMessage.setReplyMarkup(defaultKeyboard(true));
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage.setText("Enter your email, name and password." +
                                        "\nUse the following pattern: email name password");
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.REGISTRATION_WAIT_FOR_DATA);
            tgUser.setActive(false);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.REGISTRATION_WAIT_FOR_DATA) {
            String[] data;

            try {
                data = splitData(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            RegistrationDTO registrationDTO = new RegistrationDTO(data[1], data[0], data[2]);

            TokensDTO tokensDTO;
            try {
                tokensDTO = gatewayClient.createUser(registrationDTO);
            }
            catch (RuntimeException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setRefreshToken(tokensDTO.getRefreshToken());
            tgUser.setUserId(tokensDTO.getUserId());
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(true);
            telegramUserService.update(tgUser);

            sendMessage.setText("Successfully signed up");
            sendMessage.setReplyMarkup(defaultKeyboard(true));
        }
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
