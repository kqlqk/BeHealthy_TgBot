package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import lombok.Data;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyAteFoodDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyKcalsDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.OwnDailyKcalsDTO;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Scope("prototype")
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
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        DailyKcalsDTO dailyKcalsDTO = null;
        OwnDailyKcalsDTO ownDailyKcalsDTO = null;
        GeneralKcals generalKcals = new GeneralKcals();

        try {
            ownDailyKcalsDTO = gatewayClient.getOwnDailyKcalsByUserId(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException ignored) {
        }

        try {
            dailyKcalsDTO = gatewayClient.getUserDailyKcals(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException ignored) {
        }

        if (ownDailyKcalsDTO == null && dailyKcalsDTO != null) {
            generalKcals.setProtein(dailyKcalsDTO.getProtein());
            generalKcals.setFat(dailyKcalsDTO.getFat());
            generalKcals.setCarb(dailyKcalsDTO.getCarb());
        }
        else if (ownDailyKcalsDTO != null && dailyKcalsDTO != null) {
            generalKcals.setProtein(ownDailyKcalsDTO.isInPriority() ? ownDailyKcalsDTO.getProtein() : dailyKcalsDTO.getProtein());
            generalKcals.setFat(ownDailyKcalsDTO.isInPriority() ? ownDailyKcalsDTO.getFat() : dailyKcalsDTO.getFat());
            generalKcals.setCarb(ownDailyKcalsDTO.isInPriority() ? ownDailyKcalsDTO.getCarb() : dailyKcalsDTO.getCarb());
        }
        else if (ownDailyKcalsDTO != null && dailyKcalsDTO == null) {
            generalKcals.setProtein(ownDailyKcalsDTO.getProtein());
            generalKcals.setFat(ownDailyKcalsDTO.getFat());
            generalKcals.setCarb(ownDailyKcalsDTO.getCarb());
        }
        else {
            sendMessage = new SendMessage(chatId, "Please fill in your body condition or set a kilocalories goal");
            sendMessage.setReplyMarkup(initKeyboardBetweenUserConditionAndKcalsGoal());
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

        sendMessage = new SendMessage(chatId, generateText(dailyAteFoodDTOs, generalKcals));
        sendMessage.enableHtml(true);
    }

    private ReplyKeyboardMarkup initKeyboardBetweenUserConditionAndKcalsGoal() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();

        keyboardRow.add("Set condition");
        keyboardRow.add("Set only kilocalories goal");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/get_food");
        res.add("get today's food \uD83D\uDD3D");
        res.add("get today's food");

        return res;
    }

    private String generateText(List<DailyAteFoodDTO> dailyAteFoodDTOs, GeneralKcals max) {
        AtomicInteger ateKcals = new AtomicInteger();
        dailyAteFoodDTOs.forEach(e -> ateKcals.addAndGet((int) e.getKcals()));
        int maxKcals = max.getProtein() * 4 + max.getFat() * 9 + max.getCarb() * 4;

        StringBuilder text = new StringBuilder("<b>Your progress: " + ateKcals + " in " + maxKcals + " kilocalories</b>\n");
        text.append("<pre> - - - - - - - - - - - - - - - \n");
        text.append("| Name    | Kcals    | Weight |\n");
        text.append(" - - - - - - - - - - - - - - - \n");

        for (int i = dailyAteFoodDTOs.size() - 1; i >= 0; i--) {
            if (text.toString().getBytes().length > 4000) {
                break;
            }

            DailyAteFoodDTO dailyAteFoodDTO = dailyAteFoodDTOs.get(i);

            int nameLength = 7;
            if (dailyAteFoodDTO.getName().length() > nameLength) {
                text.append("| " + dailyAteFoodDTO.getName().substring(0, nameLength) + " | ");
            }
            else {
                int remainingLength = nameLength - dailyAteFoodDTO.getName().length();

                text.append("| " + dailyAteFoodDTO.getName());
                text.append(" ".repeat(remainingLength) + " | ");
            }

            int kcalsLength = 8;
            String kcalsString = String.valueOf(dailyAteFoodDTO.getKcals());
            if (kcalsString.length() > kcalsLength) {
                text.append(kcalsString.substring(0, kcalsLength) + " | ");
            }
            else {
                int remainingLength = kcalsLength - kcalsString.length();

                text.append(kcalsString);
                text.append(" ".repeat(remainingLength) + " | ");
            }

            int weightLength = 6;
            String weightString = String.valueOf(dailyAteFoodDTO.getWeight());
            if (weightString.length() > weightLength) {
                text.append(weightString.substring(0, weightLength) + " | ");
            }
            else {
                int remainingLength = weightLength - weightString.length();

                text.append(weightString);
                text.append(" ".repeat(remainingLength) + " |\n");
            }

        }

        text.append(" - - - - - - - - - - - - - - - \n</pre>");
        text.append("<b>Your progress: " + ateKcals + " in " + maxKcals + " kilocalories</b>");

        return text.toString();
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }

    @Data
    private class GeneralKcals {
        private int protein;
        private int fat;
        private int carb;
    }
}
