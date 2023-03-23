package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.AddUpdateUserKcalDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.GetDailyAteFoodDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.GetUserConditionDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.GetUserKcalDTO;
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
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@Scope("prototype")
@Slf4j
public class GetFoodCommand extends Command {
    private SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.sendObjects = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        }
        else {
            chatId = update.getMessage().getChatId().toString();
        }

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallBackQuery(update, tgUser, accessTokenDTO);
            return;
        }

        GetUserConditionDTO getUserConditionDTO = null;
        GetUserKcalDTO getUserKcalDTO = null;
        GeneralKcals generalKcals = new GeneralKcals();
        try {
            getUserKcalDTO = gatewayClient.getUserKcal(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            if (!e.getMessage().equals("You didn't set kilocalories goal")) {
                log.warn("Something went wrong", e);

                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }
        }

        if (getUserKcalDTO == null || !getUserKcalDTO.isInPriority()) {
            try {
                getUserConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                if (!e.getMessage().equals("Condition not found. Check, if you have your body's condition")) {
                    log.warn("Something went wrong", e);

                    sendMessage = new SendMessage(chatId, e.getMessage());
                    return;
                }

                if (getUserKcalDTO != null) {
                    AddUpdateUserKcalDTO addUpdateUserKcalDTO = new AddUpdateUserKcalDTO();
                    addUpdateUserKcalDTO.setKcal(getUserKcalDTO.getKcal());
                    addUpdateUserKcalDTO.setProtein(getUserKcalDTO.getProtein());
                    addUpdateUserKcalDTO.setFat(getUserKcalDTO.getFat());
                    addUpdateUserKcalDTO.setCarb(getUserKcalDTO.getCarb());
                    addUpdateUserKcalDTO.setOnlyKcal(getUserKcalDTO.isOnlyKcal());
                    addUpdateUserKcalDTO.setInPriority(true);

                    try {
                        gatewayClient.updateUserKcal(tgUser.getUserId(), addUpdateUserKcalDTO, accessTokenDTO.getAccessToken());
                    }
                    catch (RuntimeException ex) {
                        log.warn("Something went wrong", e);

                        sendMessage = new SendMessage(chatId, e.getMessage());
                        sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                        return;
                    }

                    generalKcals.setOnlyKcal(true);
                    if (!getUserKcalDTO.isOnlyKcal()) {
                        generalKcals.setProtein(getUserKcalDTO.getProtein());
                        generalKcals.setFat(getUserKcalDTO.getFat());
                        generalKcals.setCarb(getUserKcalDTO.getCarb());
                        generalKcals.setOnlyKcal(false);
                    }
                    generalKcals.setKcal(getUserKcalDTO.getKcal());
                }
                else {
                    sendMessage = new SendMessage(chatId, "Please fill in your body condition or set a kilocalories goal");
                    sendMessage.setReplyMarkup(kcalKeyboard());
                    return;
                }
            }

            if (getUserConditionDTO != null) {
                generalKcals.setOnlyKcal(false);
                generalKcals.setKcal(getUserConditionDTO.getDailyKcalDTO().getProtein() * 4 +
                                             getUserConditionDTO.getDailyKcalDTO().getFat() * 9 +
                                             getUserConditionDTO.getDailyKcalDTO().getCarb() * 4);
                generalKcals.setProtein(getUserConditionDTO.getDailyKcalDTO().getProtein());
                generalKcals.setFat(getUserConditionDTO.getDailyKcalDTO().getFat());
                generalKcals.setCarb(getUserConditionDTO.getDailyKcalDTO().getCarb());
            }
        }
        else {
            generalKcals.setOnlyKcal(true);
            if (!getUserKcalDTO.isOnlyKcal()) {
                generalKcals.setProtein(getUserKcalDTO.getProtein());
                generalKcals.setFat(getUserKcalDTO.getFat());
                generalKcals.setCarb(getUserKcalDTO.getCarb());
                generalKcals.setOnlyKcal(false);
            }
            generalKcals.setKcal(getUserKcalDTO.getKcal());
        }

        List<GetDailyAteFoodDTO> getDailyAteFoodDTOs;
        try {
            getDailyAteFoodDTOs = gatewayClient.getAllDailyAteFoods(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        sendMessage = new SendMessage(chatId, generateText(getDailyAteFoodDTOs, generalKcals));
        sendMessage.enableHtml(true);
        sendMessage.setReplyMarkup(inlineKeyboard());
    }

    private ReplyKeyboardMarkup kcalKeyboard() {
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

    private InlineKeyboardMarkup inlineKeyboard() {
        InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
        List<List<InlineKeyboardButton>> rows = new ArrayList<>();
        List<InlineKeyboardButton> row = new ArrayList<>();

        InlineKeyboardButton button = new InlineKeyboardButton("More info");
        button.setCallbackData("GetFoodCommand_" + "MoreInfo");

        row.add(button);
        rows.add(row);

        inlineKeyboardMarkup.setKeyboard(rows);

        return inlineKeyboardMarkup;
    }

    private void handleCallBackQuery(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

        List<GetDailyAteFoodDTO> getDailyAteFoodDTOs;
        try {
            getDailyAteFoodDTOs = gatewayClient.getAllDailyAteFoods(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        StringBuilder sb = new StringBuilder("<b>Detailed information:</b>\n");

        for (GetDailyAteFoodDTO getDailyAteFoodDTO : getDailyAteFoodDTOs) {
            sb.append("Name: <b>");
            sb.append(getDailyAteFoodDTO.getName());
            sb.append("</b>\n");
            sb.append("Weight: <b>");
            sb.append(getDailyAteFoodDTO.getWeight());
            sb.append("</b> g.\n");
            sb.append("Kilocalories: <b>");
            sb.append(getDailyAteFoodDTO.getKcal());
            sb.append("</b>\n");
            sb.append("Proteins: <b>");
            sb.append(getDailyAteFoodDTO.getProtein());
            sb.append("</b> g.\n");
            sb.append("Fats: <b>");
            sb.append(getDailyAteFoodDTO.getFat());
            sb.append("</b> g.\n");
            sb.append("Carbs: <b>");
            sb.append(getDailyAteFoodDTO.getCarb());
            sb.append("</b> g.\n\n");
        }

        for (int i = 0; i < sb.length(); i += 4000) {
            int endIndex = Math.min(sb.length(), i + 4000);
            String substring = sb.substring(i, endIndex);

            SendMessage sendMessage = new SendMessage(chatId, substring);
            sendMessage.enableHtml(true);
            sendObjects.add(sendMessage);
        }

    }

    private String generateText(List<GetDailyAteFoodDTO> getDailyAteFoodDTOs, GeneralKcals max) {
        AtomicInteger ateKcals = new AtomicInteger();
        getDailyAteFoodDTOs.forEach(e -> ateKcals.addAndGet(e.getKcal()));
        int maxKcals = max.getKcal();

        StringBuilder text = new StringBuilder("<b>Your progress: " + ateKcals + " in " + maxKcals + " kilocalories</b>\n");
        text.append("<pre> - - - - - - - - - - - - - - - \n");
        text.append("| Name    | Kcals    | Weight |\n");
        text.append(" - - - - - - - - - - - - - - - \n");

        for (int i = getDailyAteFoodDTOs.size() - 1; i >= 0; i--) {
            if (text.toString().getBytes().length > 4000) {
                break;
            }

            GetDailyAteFoodDTO getDailyAteFoodDTO = getDailyAteFoodDTOs.get(i);

            int maxNameLength = 7;
            if (getDailyAteFoodDTO.getName().length() > maxNameLength) {
                text.append("| ");
                text.append(getDailyAteFoodDTO.getName(), 0, maxNameLength);
                text.append(" | ");
            }
            else {
                int remainingLength = maxNameLength - getDailyAteFoodDTO.getName().length();

                text.append("| ");
                text.append(getDailyAteFoodDTO.getName());
                text.append(" ".repeat(remainingLength));
                text.append(" | ");
            }

            int maxKcalLength = 8;
            String kcalsString = String.valueOf(getDailyAteFoodDTO.getKcal());
            if (kcalsString.length() > maxKcalLength) {
                text.append(kcalsString, 0, maxKcalLength);
                text.append(" | ");
            }
            else {
                int remainingLength = maxKcalLength - kcalsString.length();

                text.append(kcalsString);
                text.append(" ".repeat(remainingLength));
                text.append(" | ");
            }

            int maxWeightLength = 6;
            String weightString = String.valueOf(getDailyAteFoodDTO.getWeight());
            if (weightString.length() > maxWeightLength) {
                text.append(weightString, 0, maxWeightLength);
                text.append(" | ");
            }
            else {
                int remainingLength = maxWeightLength - weightString.length();

                text.append(weightString);
                text.append(" ".repeat(remainingLength));
                text.append(" |\n");
            }

        }

        text.append(" - - - - - - - - - - - - - - - \n</pre>");
        text.append("<b>Your progress: ");
        text.append(ateKcals);
        text.append(" in ");
        text.append(maxKcals);
        text.append(" kilocalories</b>");

        return text.toString();
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/get_food");
        res.add("get today's food \uD83D\uDD3D");
        res.add("get today's food");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }

    @Override
    public Object[] getSendObjects() {
        return sendObjects.toArray(new Object[sendObjects.size()]);
    }

    @Data
    private class GeneralKcals {
        private boolean onlyKcal;
        private int kcal;
        private int protein;
        private int fat;
        private int carb;
    }
}
