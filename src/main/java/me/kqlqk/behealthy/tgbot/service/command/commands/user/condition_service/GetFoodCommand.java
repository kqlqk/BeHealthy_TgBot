package me.kqlqk.behealthy.tgbot.service.command.commands.user.condition_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyAteFoodDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Service
public class GetFoodCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck

    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        List<DailyAteFoodDTO> dailyAteFoodDTOs;

        try {
            dailyAteFoodDTOs = gatewayClient.getDailyAteFoods(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        StringBuilder text = new StringBuilder("Your food: \n");

        for (DailyAteFoodDTO dailyAteFoodDTO : dailyAteFoodDTOs) {
            text.append("Name: " + dailyAteFoodDTO.getName() + "\n")
                    .append("Weight: " + dailyAteFoodDTO.getWeight() + "g \n")
                    .append("Kilocalories: " + dailyAteFoodDTO.getKcals() + " \n")
                    .append("Proteins: " + dailyAteFoodDTO.getProteins() + "g. \n")
                    .append("Fats: " + dailyAteFoodDTO.getFats() + "g. \n")
                    .append("Carbs: " + dailyAteFoodDTO.getCarbs() + "g. \n\n");
        }

        sendMessage = new SendMessage(chatId, text.toString());
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
