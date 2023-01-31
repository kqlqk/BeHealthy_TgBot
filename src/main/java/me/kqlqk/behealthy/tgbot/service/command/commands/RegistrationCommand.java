package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.UserDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.TreeMap;

@Service
public class RegistrationCommand implements Command {
    private final GatewayClient gatewayClient;
    private final TelegramUserService telegramUserService;
    private final TreeMap<Long, UserDTO> tgIdUserDTO = new TreeMap<>();

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
        String text;
        UserDTO userDTO;

        while (tgIdUserDTO.size() > 20) {
            long tgId = tgIdUserDTO.pollFirstEntry().getKey();
            TelegramUser telegramUser = telegramUserService.getByTelegramId(tgId);
            telegramUser.setCommandSate(CommandState.BASIC);
            telegramUser.setActive(false);
            telegramUserService.update(telegramUser);
        }

        if (tgUser.isActive()) {
            text = "You already signed up";

            sendMessage = new SendMessage(chatId, text);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            text = "Enter your email | Only valid email";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.REGISTRATION_WAIT_FOR_EMAIL);
            tgUser.setActive(false);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.REGISTRATION_WAIT_FOR_EMAIL) {
            userDTO = new UserDTO();
            userDTO.setEmail(userMessage);
            tgIdUserDTO.put(tgUser.getTelegramId(), userDTO);

            text = "Enter your name | Name can contains only letters";
            sendMessage = new SendMessage(chatId, text);
            tgUser.setCommandSate(CommandState.REGISTRATION_WAIT_FOR_NAME);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.REGISTRATION_WAIT_FOR_NAME) {
            userDTO = tgIdUserDTO.get(tgUser.getTelegramId());
            userDTO.setName(userMessage);

            text = "Enter your password | " +
                    "Password should be between 8 and 50 characters, at least: 1 number, 1 uppercase letter, 1 lowercase letter";
            sendMessage = new SendMessage(chatId, text);
            tgUser.setCommandSate(CommandState.REGISTRATION_WAIT_FOR_PASSWORD);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.REGISTRATION_WAIT_FOR_PASSWORD) {
            userDTO = tgIdUserDTO.get(tgUser.getTelegramId());
            userDTO.setPassword(userMessage);
            tgIdUserDTO.remove(tgUser.getTelegramId());

            TokensDTO tokensDTO = gatewayClient.createUser(userDTO); //TODO handle

            tgUser.setRefreshToken(tokensDTO.getRefreshToken());
            tgUser.setUserId(tokensDTO.getUserId());
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(true);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(update.getMessage().getChatId().toString(), "Successfully signed up");
            return;
        }

        sendMessage = null;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
