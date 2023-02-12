package me.kqlqk.behealthy.tgbot.service.command.commands.user.condition_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyKcalsDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class DailyKcalsCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public DailyKcalsCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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

        DailyKcalsDTO dailyKcalsDTO;

        try {
            dailyKcalsDTO = gatewayClient.getUserDailyKcals(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        int kcals = dailyKcalsDTO.getProtein() * 4 + dailyKcalsDTO.getFat() * 9 + dailyKcalsDTO.getCarb() * 4;
        UserConditionDTO userConditionDTO;

        try {
            userConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        String goal = userConditionDTO.getGoal().equals("LOSE") ? "cutting" :
                userConditionDTO.getGoal().equals("MAINTAIN") ? "maintaining" : "bulking";


        String text = "Your daily kilocalories for " + goal + " should be: " + kcals + " in which: \n" +
                "Proteins: " + dailyKcalsDTO.getProtein() + "g. \n" +
                "Fats: " + dailyKcalsDTO.getFat() + "g. \n" +
                "Carbs: " + dailyKcalsDTO.getCarb() + "g. \n";

        sendMessage = new SendMessage(chatId, text);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
