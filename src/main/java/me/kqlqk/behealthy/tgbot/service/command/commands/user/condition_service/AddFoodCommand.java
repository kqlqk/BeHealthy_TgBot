package me.kqlqk.behealthy.tgbot.service.command.commands.user.condition_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyAteFoodDTO;
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

@Service
public class AddFoodCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public AddFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            String text = "Add food. \nUse the following pattern: name weight proteins(per 100g) fats(per 100g) carbs(per 100g)";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.ADD_FOOD_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.ADD_FOOD_WAIT_FOR_DATA) {
            String[] food;

            try {
                food = splitFood(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            DailyAteFoodDTO dailyAteFoodDTO = new DailyAteFoodDTO(
                    food[0],
                    Double.parseDouble(food[1]),
                    Double.parseDouble(food[2]),
                    Double.parseDouble(food[3]),
                    Double.parseDouble(food[4]));

            try {
                gatewayClient.addDailyAteFoods(tgUser.getUserId(), dailyAteFoodDTO, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(chatId, "Food was successfully added");
            return;
        }

        sendMessage = null;
    }

    private String[] splitFood(String data) {
        String[] split = data.split(" ");

        if (split.length < 5) {
            throw new BadUserDataException("Please, use the following pattern: name weight proteins(per 100g) fats(per 100g) carbs(per 100g)");
        }

        try {
            Double.parseDouble(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Double.parseDouble(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'proteins' was not provided a number");
        }

        try {
            Double.parseDouble(split[3]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'fats' was not provided a number");
        }

        try {
            Double.parseDouble(split[4]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'carbs' was not provided a number");
        }

        return split;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
