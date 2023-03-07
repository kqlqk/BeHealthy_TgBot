package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.AddUpdateUserKcalDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.GetUserConditionDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.GetUserKcalDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
import me.kqlqk.behealthy.tgbot.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class ChangeKcalGoalCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public ChangeKcalGoalCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage = new SendMessage(chatId, "Would you like to get your kilocalories goal from our system or set it yourself?");
            sendMessage.setReplyMarkup(initKeyboard());

            tgUser.setCommandSate(CommandState.CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING) {
            if (userMessage.equalsIgnoreCase("Do it for me")) {
                GetUserKcalDTO getUserKcalDTO = null;

                try {
                    getUserKcalDTO = gatewayClient.getUserKcal(tgUser.getUserId(), accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    if (!e.getMessage().equals("You didn't set kilocalories goal")) {
                        log.error("Something went wrong", e);

                        sendMessage = new SendMessage(chatId, e.getMessage());
                        return;
                    }
                }

                if (getUserKcalDTO != null) {
                    AddUpdateUserKcalDTO updateUserKcalDTO = new AddUpdateUserKcalDTO();
                    updateUserKcalDTO.setKcal(getUserKcalDTO.getKcal());
                    updateUserKcalDTO.setProtein(getUserKcalDTO.getProtein());
                    updateUserKcalDTO.setFat(getUserKcalDTO.getFat());
                    updateUserKcalDTO.setCarb(getUserKcalDTO.getCarb());
                    updateUserKcalDTO.setOnlyKcal(getUserKcalDTO.isOnlyKcal());
                    updateUserKcalDTO.setInPriority(false);

                    try {
                        gatewayClient.updateUserKcal(tgUser.getUserId(), updateUserKcalDTO, accessTokenDTO.getAccessToken());
                    }
                    catch (RuntimeException e) {
                        sendMessage = new SendMessage(chatId, e.getMessage());
                        sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

                        tgUser.setCommandSate(CommandState.BASIC);
                        telegramUserService.update(tgUser);
                        return;
                    }
                }

                GetUserConditionDTO getUserConditionDTO = null;
                try {
                    getUserConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    if (!e.getMessage().equals("Condition not found. Check, if you have your body's condition")) {
                        log.error("Something went wrong", e);

                        sendMessage = new SendMessage(chatId, e.getMessage());
                        return;
                    }
                }

                if (getUserConditionDTO == null) {
                    sendMessage = new SendMessage(chatId, "Please fill in your body condition " +
                            "so that we can generate kilocalories goal for you");
                }
                else {
                    sendMessage = new SendMessage(chatId, "Done. \n" +
                            "If you want to update it, you should update your body condition");
                }

                sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());
                tgUser.setCommandSate(CommandState.BASIC);
                telegramUserService.update(tgUser);
            }
            else if (userMessage.equalsIgnoreCase("I will set myself")) {
                String text = "Let's set your kilocalories goal.\n" +
                        "Do you want to set only kilocalories or proteins fats carbs too?";
                sendMessage = new SendMessage(chatId, text);
                sendMessage.setReplyMarkup(kcalKeyboard());

                tgUser.setCommandSate(CommandState.CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING_KCAL);
                telegramUserService.update(tgUser);
            }
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING_KCAL) {
            if (userMessage.equalsIgnoreCase("Only kilocalories")) {
                Maps.putUserIdOnlyKcal(tgUser.getUserId(), true);
                sendMessage = new SendMessage(chatId, "Use the following pattern: kilocalories");
            }
            else if (userMessage.equalsIgnoreCase("Proteins, fats, carbs too")) {
                Maps.putUserIdOnlyKcal(tgUser.getUserId(), false);
                sendMessage = new SendMessage(chatId, "Use the following pattern: proteins fats carbs");
            }

            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.CHANGE_KCAL_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_KCAL_WAIT_FOR_DATA) {
            String[] dailyKcal;
            AddUpdateUserKcalDTO addUserKcalDTO = new AddUpdateUserKcalDTO();
            addUserKcalDTO.setOnlyKcal(Maps.getUserIdOnlyKcal(tgUser.getUserId()));
            addUserKcalDTO.setInPriority(true);

            try {
                dailyKcal = splitDailyKcal(userMessage, addUserKcalDTO.isOnlyKcal());
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            if (addUserKcalDTO.isOnlyKcal()) {
                addUserKcalDTO.setKcal(Integer.parseInt(dailyKcal[0]));
            }
            else {
                int protein = Integer.parseInt(dailyKcal[0]);
                int fat = Integer.parseInt(dailyKcal[1]);
                int carb = Integer.parseInt(dailyKcal[2]);

                addUserKcalDTO.setKcal(protein * 4 + fat * 9 + carb * 4);
                addUserKcalDTO.setProtein(protein);
                addUserKcalDTO.setFat(fat);
                addUserKcalDTO.setCarb(carb);
            }

            GetUserKcalDTO getUserKcalDTO = null;
            try {
                getUserKcalDTO = gatewayClient.getUserKcal(tgUser.getUserId(), accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                if (!e.getMessage().equals("You didn't set kilocalories goal")) {
                    log.error("Something went wrong", e);

                    sendMessage = new SendMessage(chatId, e.getMessage());
                    sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());
                    tgUser.setCommandSate(CommandState.BASIC);
                    telegramUserService.update(tgUser);
                    return;
                }
            }

            if (getUserKcalDTO == null) {
                try {
                    gatewayClient.addUserKcal(tgUser.getUserId(), addUserKcalDTO, accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    sendMessage = new SendMessage(chatId, e.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }
            }
            else {
                try {
                    gatewayClient.updateUserKcal(tgUser.getUserId(), addUserKcalDTO, accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    sendMessage = new SendMessage(chatId, e.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }
            }

            Maps.removeUserIdOnlyKcal(tgUser.getUserId());

            sendMessage = new SendMessage(chatId, "Successfully added");
            sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);
        }
    }

    private ReplyKeyboardMarkup kcalKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Only kilocalories");
        keyboardRow.add("Proteins, fats, carbs too");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Do it for me");
        keyboardRow.add("I will set myself");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String[] splitDailyKcal(String data, boolean isOnlyKcal) {
        String[] split = data.split(" ");

        if (isOnlyKcal) {
            if (split.length < 1) {
                throw new BadUserDataException("Please, use the following pattern: kilocalories");
            }

            try {
                Integer.parseInt(split[0]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'kilocalories' was not provided a number");
            }
        }
        else {
            if (split.length < 3) {
                throw new BadUserDataException("Please, use the following pattern: proteins fats carbs");
            }

            try {
                Integer.parseInt(split[0]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'proteins' was not provided a number");
            }

            try {
                Integer.parseInt(split[1]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'fats' was not provided a number");
            }

            try {
                Integer.parseInt(split[2]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'carbs' was not provided a number");
            }
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/change_kcals_goal");
        res.add("change kilocalories goal ⚙");
        res.add("change kilocalories goal");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
