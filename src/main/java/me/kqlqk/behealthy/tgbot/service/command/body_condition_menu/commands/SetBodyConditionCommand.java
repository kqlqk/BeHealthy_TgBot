package me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.AddUpdateUserConditionDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.BodyConditionMenu;
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
public class SetBodyConditionCommand extends Command {
    private final SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final BodyConditionMenu bodyConditionMenu;

    @Autowired
    public SetBodyConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, BodyConditionMenu bodyConditionMenu) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.bodyConditionMenu = bodyConditionMenu;
        sendObjects = new ArrayList<>();
        sendMessage = new SendMessage();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        sendMessage.setChatId(chatId);

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage.setText("Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC || tgUser.getCommandSate() == CommandState.RETURN_TO_BODY_CONDITION_MENU) {
            String text = "Do you know your percent of body fat?";
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(yesNoKeyboard());

            tgUser.setCommandSate(CommandState.SET_BODY_CONDITION_WAIT_FOR_FAT_PERCENT);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_BODY_CONDITION_WAIT_FOR_FAT_PERCENT) {
            if (userMessage.equalsIgnoreCase("Yes")) {
                Maps.putUserIdFatPercent(tgUser.getUserId(), true);
            }
            else if (userMessage.equalsIgnoreCase("No")) {
                Maps.putUserIdFatPercent(tgUser.getUserId(), false);
            }
            else {
                sendMessage.setText("Waiting for answer: yes / no");
                sendMessage.setReplyMarkup(yesNoKeyboard());
                return;
            }

            sendMessage.setText("What's your gender?");
            sendMessage.setReplyMarkup(genderKeyboard());

            tgUser.setCommandSate(CommandState.SET_BODY_CONDITION_WAIT_FOR_GENDER);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_BODY_CONDITION_WAIT_FOR_GENDER) {
            if (userMessage.equalsIgnoreCase("Male")) {
                Maps.putUserIdGender(tgUser.getUserId(), "MALE");
            }
            else if (userMessage.equalsIgnoreCase("Female")) {
                Maps.putUserIdGender(tgUser.getUserId(), "FEMALE");
            }
            else {
                sendMessage.setText("Waiting for answer: male / female");
                sendMessage.setReplyMarkup(genderKeyboard());
                return;
            }

            sendMessage.setText("What's your daily activity:\n" +
                                        "* minimal (no gym / sitting work)\n" +
                                        "* average (2-3 times per week)\n" +
                                        "* maximal (4-6 times per week)");
            sendMessage.setReplyMarkup(activityKeyboard());

            tgUser.setCommandSate(CommandState.SET_BODY_CONDITION_WAIT_FOR_ACTIVITY);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_BODY_CONDITION_WAIT_FOR_ACTIVITY) {
            if (userMessage.equalsIgnoreCase("minimal")) {
                Maps.putUserIdActivity(tgUser.getUserId(), "MIN");
            }
            else if (userMessage.equalsIgnoreCase("average")) {
                Maps.putUserIdActivity(tgUser.getUserId(), "AVG");
            }
            else if (userMessage.equalsIgnoreCase("maximal")) {
                Maps.putUserIdActivity(tgUser.getUserId(), "MAX");
            }
            else {
                sendMessage.setText("Waiting for answer: minimal / average / maximal");
                sendMessage.setReplyMarkup(activityKeyboard());
                return;
            }

            sendMessage.setText("What's your goal?");
            sendMessage.setReplyMarkup(goalKeyboard());

            tgUser.setCommandSate(CommandState.SET_BODY_CONDITION_WAIT_FOR_GOAL);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_BODY_CONDITION_WAIT_FOR_GOAL) {
            if (userMessage.equalsIgnoreCase("Lose weight")) {
                Maps.putUserIdGoal(tgUser.getUserId(), "LOSE");
            }
            else if (userMessage.equalsIgnoreCase("Maintain weight")) {
                Maps.putUserIdGoal(tgUser.getUserId(), "MAINTAIN");
            }
            else if (userMessage.equalsIgnoreCase("Gain weight")) {
                Maps.putUserIdGoal(tgUser.getUserId(), "GAIN");
            }
            else {
                sendMessage.setText("Waiting for answer: Lose weight / Maintain weight / Gain weight");
                sendMessage.setReplyMarkup(goalKeyboard());
                return;
            }

            if (Maps.getUserIdFatPercent(tgUser.getUserId())) {
                sendMessage.setText("Use the following pattern: age height weight 'fat percent'");
            }
            else {
                if (Maps.getUserIdGender(tgUser.getUserId()).equalsIgnoreCase("male")) {
                    sendMessage.setText("Use the following pattern: age height weight " +
                                                "'fat fold between chest and ilium' " +
                                                "'fat fold between navel and lower belly' " +
                                                "'fat fold between nipple and armpit' " +
                                                "'fat fold between nipple and upper chest");
                }
                else {
                    sendMessage.setText("Use the following pattern: age height weight " +
                                                "'fat fold between shoulder and elbow' " +
                                                "'fat fold between chest and ilium' " +
                                                "'fat fold between navel and lower belly' ");
                }
            }

            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.SET_BODY_CONDITION_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_BODY_CONDITION_WAIT_FOR_DATA) {
            String[] data;

            try {
                data = split(userMessage, Maps.getUserIdFatPercent(tgUser.getUserId()), Maps.getUserIdGender(tgUser.getUserId()));
            }
            catch (BadUserDataException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            AddUpdateUserConditionDTO addUpdateUserConditionDTO = new AddUpdateUserConditionDTO();
            addUpdateUserConditionDTO.setGoal(Maps.getUserIdGoal(tgUser.getUserId()));
            addUpdateUserConditionDTO.setActivity(Maps.getUserIdActivity(tgUser.getUserId()));
            addUpdateUserConditionDTO.setAge(Integer.parseInt(data[0]));
            addUpdateUserConditionDTO.setHeight(Integer.parseInt(data[1]));
            addUpdateUserConditionDTO.setWeight(Integer.parseInt(data[2]));

            if (Maps.getUserIdFatPercent(tgUser.getUserId())) {
                addUpdateUserConditionDTO.setGender(Maps.getUserIdGender(tgUser.getUserId()));
                addUpdateUserConditionDTO.setFatPercentExists(true);
                addUpdateUserConditionDTO.setFatPercent(Double.parseDouble(data[3]));
            }
            else {
                addUpdateUserConditionDTO.setFatPercentExists(false);

                if (Maps.getUserIdGender(tgUser.getUserId()).equalsIgnoreCase("male")) {
                    addUpdateUserConditionDTO.setGender("MALE");
                    addUpdateUserConditionDTO.setFatFoldBetweenChestAndIlium(Integer.parseInt(data[3]));
                    addUpdateUserConditionDTO.setFatFoldBetweenNavelAndLowerBelly(Integer.parseInt(data[4]));
                    addUpdateUserConditionDTO.setFatFoldBetweenNippleAndArmpit(Integer.parseInt(data[5]));
                    addUpdateUserConditionDTO.setFatFoldBetweenNippleAndUpperChest(Integer.parseInt(data[6]));
                }
                else {
                    addUpdateUserConditionDTO.setGender("FEMALE");
                    addUpdateUserConditionDTO.setFatFoldBetweenShoulderAndElbow(Integer.parseInt(data[3]));
                    addUpdateUserConditionDTO.setFatFoldBetweenChestAndIlium(Integer.parseInt(data[4]));
                    addUpdateUserConditionDTO.setFatFoldBetweenNavelAndLowerBelly(Integer.parseInt(data[5]));
                }
            }

            try {
                gatewayClient.createUserCondition(tgUser.getUserId(), addUpdateUserConditionDTO, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                if (!e.getMessage().equals("You already set a body condition")) {
                    sendMessage.setText(e.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }

                try {
                    gatewayClient.updateUserCondition(tgUser.getUserId(), addUpdateUserConditionDTO, accessTokenDTO.getAccessToken());
                }
                catch (RuntimeException ex) {
                    sendMessage.setText(ex.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            Maps.removeUserIdActivity(tgUser.getUserId());
            Maps.removeUserIdGender(tgUser.getUserId());
            Maps.removeUserIdGoal(tgUser.getUserId());
            Maps.removeUserIdFatPercent(tgUser.getUserId());

            bodyConditionMenu.handle(update, tgUser, accessTokenDTO, securityState);

            SendMessage sendMessage1 = new SendMessage(chatId, "Successfully");
            SendMessage sendMessage2 = bodyConditionMenu.getSendMessage();
            sendMessage2.setReplyMarkup(BodyConditionMenu.initKeyboard());
            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    private static ReplyKeyboardMarkup yesNoKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Yes");
        keyboardRow.add("No");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private static ReplyKeyboardMarkup genderKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Male");
        keyboardRow.add("Female");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private static ReplyKeyboardMarkup activityKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Minimal");
        keyboardRow.add("Average");
        keyboardRow.add("Maximal");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private static ReplyKeyboardMarkup goalKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Lose weight");
        keyboardRow.add("Maintain weight");
        keyboardRow.add("Gain weight");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String[] split(String data, boolean hasFatPercent, String gender) {
        String[] split = data.split(" ");

        if (hasFatPercent) {
            if (split.length < 4) {
                throw new BadUserDataException("Please, use the following pattern: age height weight 'fat percent'");
            }

            try {
                Integer.parseInt(split[0]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'age' was not provided a number");
            }

            try {
                Integer.parseInt(split[1]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'height' was not provided a number");
            }

            try {
                Integer.parseInt(split[2]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'weight' was not provided a number");
            }

            try {
                Double.parseDouble(split[3]);
            }
            catch (NumberFormatException e) {
                throw new BadUserDataException("For 'fat percent' was not provided a number");
            }
        }
        else {
            if (gender.equalsIgnoreCase("male")) {
                if (split.length < 7) {
                    throw new BadUserDataException("Please, use the following pattern: age height weight " +
                                                           "'fat fold between chest and ilium' " +
                                                           "'fat fold between navel and lower belly' " +
                                                           "'fat fold between nipple and armpit' " +
                                                           "'fat fold between nipple and upper chest'");
                }

                try {
                    Integer.parseInt(split[0]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'age' was not provided a number");
                }

                try {
                    Integer.parseInt(split[1]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'height' was not provided a number");
                }

                try {
                    Integer.parseInt(split[2]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'weight' was not provided a number");
                }

                try {
                    Integer.parseInt(split[3]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between chest and ilium' was not provided a number");
                }

                try {
                    Integer.parseInt(split[4]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between navel and lower belly' was not provided a number");
                }

                try {
                    Integer.parseInt(split[5]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between nipple and armpit' was not provided a number");
                }

                try {
                    Integer.parseInt(split[6]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between nipple and upper chest' was not provided a number");
                }
            }
            else {
                if (split.length < 7) {
                    throw new BadUserDataException("Please, use the following pattern: age height weight " +
                                                           "'fat fold between shoulder and elbow' " +
                                                           "'fat fold between chest and ilium'" +
                                                           "'fat fold between navel and lower belly'");
                }

                try {
                    Integer.parseInt(split[0]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'age' was not provided a number");
                }

                try {
                    Integer.parseInt(split[1]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'height' was not provided a number");
                }

                try {
                    Integer.parseInt(split[2]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'weight' was not provided a number");
                }

                try {
                    Integer.parseInt(split[3]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between shoulder and elbow' was not provided a number");
                }

                try {
                    Integer.parseInt(split[4]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between chest and ilium' was not provided a number");
                }

                try {
                    Integer.parseInt(split[5]);
                }
                catch (NumberFormatException e) {
                    throw new BadUserDataException("For 'fat fold between navel and lower belly' was not provided a number");
                }
            }
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("set condition");
        res.add("change condition");
        res.add("/set_body_condition");
        res.add("/change_body_condition");

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
}
