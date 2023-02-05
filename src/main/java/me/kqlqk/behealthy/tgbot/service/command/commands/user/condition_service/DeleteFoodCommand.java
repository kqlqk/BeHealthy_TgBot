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
public class DeleteFoodCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public DeleteFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            String text = "Enter food name to remove it";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.DELETE_FOOD_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.DELETE_FOOD_WAIT_FOR_DATA) {
            List<DailyAteFoodDTO> dailyAteFoodDTOs;

            try {
                dailyAteFoodDTOs = gatewayClient.getDailyAteFoods(tgUser.getUserId(), tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            boolean wasFood = false;
            for (DailyAteFoodDTO dailyAteFoodDTO : dailyAteFoodDTOs) {
                if (dailyAteFoodDTO.getName().equalsIgnoreCase(userMessage)) {
                    try {
                        wasFood = true;
                        gatewayClient.deleteDailyAteFood(tgUser.getUserId(), dailyAteFoodDTO.getId(), tokensDTO.getAccessToken());
                    }
                    catch (RuntimeException e) {
                        sendMessage = new SendMessage(chatId, e.getMessage());
                        return;
                    }
                }
            }

            if (!wasFood) {
                sendMessage = new SendMessage(chatId, "Food  '" + userMessage + "' not found.\nEnter food name to remove it");
                return;
            }

            sendMessage = new SendMessage(chatId, "Food successfully deleted.");

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);
            return;
        }

        sendMessage = null;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
