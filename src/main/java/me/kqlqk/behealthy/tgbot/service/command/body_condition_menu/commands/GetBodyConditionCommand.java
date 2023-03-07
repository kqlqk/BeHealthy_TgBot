package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.GetUserConditionDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class GetBodyConditionCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetBodyConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        tgUser.setCommandSate(CommandState.RETURN_TO_BODY_CONDITION_MENU);

        telegramUserService.update(tgUser);

        GetUserConditionDTO getUserConditionDTO;
        try {
            getUserConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            if (!e.getMessage().equals("Condition not found. Check, if you have your body's condition")) {
                log.error("Something went wrong", e);

                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            sendMessage = new SendMessage(chatId, "Please, set your body condition");
            sendMessage.setReplyMarkup(setConditionKeyboard());

            return;
        }

        sendMessage = new SendMessage(chatId, generateText(getUserConditionDTO));
        sendMessage.enableHtml(true);
        sendMessage.setReplyMarkup(initKeyboard());
    }

    private static ReplyKeyboardMarkup setConditionKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Set condition");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private static ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Change condition");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String generateText(GetUserConditionDTO getUserConditionDTO) {
        StringBuilder text = new StringBuilder("<b>Your body:</b>\n");
        text.append("<pre>Gender: " + StringUtils.capitalize(getUserConditionDTO.getGender().toLowerCase()) + "\n");
        text.append("Age: " + getUserConditionDTO.getAge() + " y.o.\n");
        text.append("Weight: " + getUserConditionDTO.getWeight() + " kg.\n");
        text.append("Fat: " + getUserConditionDTO.getFatPercent() + " %\n");
        text.append("Goal: " + setStringGoal(getUserConditionDTO.getGoal()) + "\n");
        text.append("Intensity: " + setStringIntensity(getUserConditionDTO.getIntensity()) + "</pre>\n\n");
        text.append("<b>Recommended kilocalories:</b>\n");

        int protein = getUserConditionDTO.getDailyKcalDTO().getProtein();
        int fat = getUserConditionDTO.getDailyKcalDTO().getFat();
        int carb = getUserConditionDTO.getDailyKcalDTO().getCarb();

        text.append("<pre>Kilocalories: " + (protein * 4 + fat * 9 + carb * 4) + "\n");
        text.append("Protein: " + protein + " g.\n");
        text.append("Fat: " + fat + " g.\n");
        text.append("Carb: " + carb + " g.</pre>\n");

        return text.toString();
    }

    private String setStringGoal(String goal) {
        switch (goal) {
            case "LOSE":
                return "Lose weight";

            case "MAINTAIN":
                return "Maintain current weight";

            case "GAIN":
                return "Gain weight";

            default:
                return "";
        }
    }

    private String setStringIntensity(String intensity) {
        switch (intensity) {
            case "MIN":
                return "Minimal activity";

            case "AVG":
                return "2-3 activities per week";

            case "MAX":
                return "4-6 activities per week";

            default:
                return "";
        }
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("body condition \uD83D\uDD76");
        res.add("body condition");
        res.add("/get_body_condition");

        return res;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
