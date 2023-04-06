package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.GetUserConditionDTO;
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
public class GetBodyConditionCommand extends Command {
    private final SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetBodyConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        sendMessage = new SendMessage();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage.setText("Sorry, you should sign in again");
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

                sendMessage.setText(e.getMessage());
                return;
            }

            sendMessage.setText("Please, set your body condition");
            sendMessage.setReplyMarkup(setConditionKeyboard());

            return;
        }

        sendMessage.setText(generateText(getUserConditionDTO));
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
        text.append("<pre>Gender: ");
        text.append(StringUtils.capitalize(getUserConditionDTO.getGender().toLowerCase()));
        text.append("\nAge: ");
        text.append(getUserConditionDTO.getAge());
        text.append(" y.o.");
        text.append("\nWeight: ");
        text.append(getUserConditionDTO.getWeight());
        text.append(" kg.");
        text.append("\nFat: ");
        text.append(getUserConditionDTO.getFatPercent());
        text.append(" %");
        text.append("\nGoal: ");
        text.append(setStringGoal(getUserConditionDTO.getGoal()));
        text.append("\nActivity: ");
        text.append(setStringActivity(getUserConditionDTO.getActivity()));
        text.append("</pre>");
        text.append("\n\n<b>Recommended kilocalories:</b>\n");

        int protein = getUserConditionDTO.getDailyKcalDTO().getProtein();
        int fat = getUserConditionDTO.getDailyKcalDTO().getFat();
        int carb = getUserConditionDTO.getDailyKcalDTO().getCarb();

        text.append("<pre>Kilocalories: ");
        text.append((protein * 4 + fat * 9 + carb * 4));
        text.append("\nProtein: ");
        text.append(protein);
        text.append(" g.");
        text.append("\nFat: ");
        text.append(fat);
        text.append(" g.");
        text.append("\nCarb: ");
        text.append(carb);
        text.append(" g.</pre>\n");

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

    private String setStringActivity(String activity) {
        switch (activity) {
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
