package me.kqlqk.behealthy.tgbot.service.command.user_commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.GetUserDTO;
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

@Service
@Slf4j
@Scope("prototype")
public class MeCommand extends Command {
    private SendMessage sendMessage;
    private final GatewayClient gatewayClient;
    private final TelegramUserService telegramUserService;

    @Autowired
    public MeCommand(GatewayClient gatewayClient, TelegramUserService telegramUserService) {
        this.gatewayClient = gatewayClient;
        this.telegramUserService = telegramUserService;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        GetUserDTO getUserDTO;

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        try {
            getUserDTO = gatewayClient.getUser(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            log.error("Something went wrong", e);
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        String text = "Name: " + getUserDTO.getName() + "\nEmail: " + getUserDTO.getEmail();

        sendMessage = new SendMessage(chatId, text);
        sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
