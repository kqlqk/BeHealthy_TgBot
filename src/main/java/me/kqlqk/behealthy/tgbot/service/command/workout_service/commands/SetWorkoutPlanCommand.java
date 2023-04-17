package me.kqlqk.behealthy.tgbot.service.command.workout_service.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_service.AddUpdateWorkoutInfoDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
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

@Service
@Scope("prototype")
@Slf4j
public class SetWorkoutPlanCommand extends Command {
    private final SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetWorkoutPlanCommand getWorkoutPlanCommand;

    @Autowired
    public SetWorkoutPlanCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetWorkoutPlanCommand getWorkoutPlanCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.getWorkoutPlanCommand = getWorkoutPlanCommand;
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

        if (tgUser.getCommandSate() == CommandState.BASIC || tgUser.getCommandSate() == CommandState.RETURN_TO_WORKOUT_SERVICE_MENU) {
            String text = "How many times a week do you want to workout?";
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(workoutKeyboard());

            tgUser.setCommandSate(CommandState.SET_WORKOUT_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SET_WORKOUT_WAIT_FOR_DATA) {
            int times;

            try {
                times = splitData(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(workoutKeyboard());
                return;
            }

            AddUpdateWorkoutInfoDTO addUpdateWorkoutInfoDTO = new AddUpdateWorkoutInfoDTO(times);

            try {
                gatewayClient.createWorkoutInfos(tgUser.getUserId(), addUpdateWorkoutInfoDTO, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                log.error("Something went wrong", e);

                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            getWorkoutPlanCommand.handle(update, tgUser, accessTokenDTO, securityState);

            SendMessage sendMessage1 = new SendMessage(chatId, "Successfully");
            SendMessage sendMessage2 = getWorkoutPlanCommand.getSendMessage();
            sendMessage2.setReplyMarkup(GetWorkoutPlanCommand.initKeyboard());
            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    private static ReplyKeyboardMarkup workoutKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("1");
        keyboardRow.add("2");
        keyboardRow.add("3");
        keyboardRow.add("4");
        keyboardRow.add("5");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private int splitData(String data) {
        int res;
        try {
            res = Integer.parseInt(data);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("Was not provided a number");
        }

        if (res < 1 || res > 5) {
            throw new BadUserDataException("You should choose between 1 to 5 workouts per week");
        }

        return res;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("set workout");
        res.add("change workout");
        res.add("/set_workout");
        res.add("/change_workout");

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
