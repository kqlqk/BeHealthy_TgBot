package me.kqlqk.behealthy.tgbot.service.command.commands.user;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class GetConditionCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        UserConditionDTO userConditionDTO;

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        try {
            userConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        String text = "Gender: " + userConditionDTO.getGender() +
                "\nAge: " + userConditionDTO.getAge() +
                "\nHeight: " + userConditionDTO.getHeight() +
                "\nWeight: " + userConditionDTO.getWeight() +
                "\nIntensity: " + userConditionDTO.getIntensity() +
                "\nGoal: " + userConditionDTO.getGoal() +
                "\nFat percent: " + userConditionDTO.getFatPercent();

        sendMessage = new SendMessage(chatId, text);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}