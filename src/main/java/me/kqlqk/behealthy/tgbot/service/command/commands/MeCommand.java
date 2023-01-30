package me.kqlqk.behealthy.tgbot.service.command.commands;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.UserDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class MeCommand implements Command {
    private SendMessage sendMessage;
    private final GatewayClient gatewayClient;

    @Autowired
    public MeCommand(GatewayClient gatewayClient) {
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO accessTokenDTO) {
        UserDTO userDTO = gatewayClient.getUser(tgUser.getUserId(), "Bearer " + accessTokenDTO.getAccessToken()); //TODO handle
        String text = "name: " + userDTO.getName() + "\n" +
                "email: " + userDTO.getEmail();

        sendMessage = new SendMessage(update.getMessage().getChatId().toString(), text);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
