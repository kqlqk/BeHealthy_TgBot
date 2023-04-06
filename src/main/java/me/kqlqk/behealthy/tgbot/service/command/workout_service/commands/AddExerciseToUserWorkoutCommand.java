package me.kqlqk.behealthy.tgbot.service.command.workout_service.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_service.AddUserWorkoutDTO;
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

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class AddExerciseToUserWorkoutCommand extends Command {
    private final SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetUserWorkoutCommand getUserWorkoutCommand;

    @Autowired
    public AddExerciseToUserWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetUserWorkoutCommand getUserWorkoutCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.getUserWorkoutCommand = getUserWorkoutCommand;
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
            String text = "Let's add your exercise to plan\n" +
                    "Use the following pattern: 'Day of the week (from 1 to 7) 'exercise order' 'exercise name' reps sets";
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.ADD_EXERCISE_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.ADD_EXERCISE_WAIT_FOR_DATA) {
            String[] data;

            try {
                data = split(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            AddUserWorkoutDTO addUserWorkoutDTO = new AddUserWorkoutDTO();
            addUserWorkoutDTO.setDay(Integer.parseInt(data[0]));
            addUserWorkoutDTO.setNumberPerDay(Integer.parseInt(data[1]));
            addUserWorkoutDTO.setExerciseName(data[2]);
            addUserWorkoutDTO.setRep(Integer.parseInt(data[3]));
            addUserWorkoutDTO.setSet(Integer.parseInt(data[4]));

            try {
                gatewayClient.addExerciseToUserWorkout(tgUser.getUserId(), addUserWorkoutDTO, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            getUserWorkoutCommand.handle(update, tgUser, accessTokenDTO, securityState);

            SendMessage sendMessage1 = new SendMessage(chatId, "Successfully");
            SendMessage sendMessage2 = getUserWorkoutCommand.getSendMessage();
            sendMessage2.setReplyMarkup(GetUserWorkoutCommand.initKeyboard());
            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    private String[] split(String userMessage) {
        String[] data = userMessage.split(" ");

        if (data.length < 5) {
            throw new BadUserDataException("Please, use the following pattern: 'day of the week (from 1 to 7) 'exercise order' 'exercise name' reps sets");
        }

        try {
            Integer.parseInt(data[0]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'day of the week' was not provided a number");
        }

        try {
            Integer.parseInt(data[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'exercise order' was not provided a number");
        }

        try {
            Integer.parseInt(data[3]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'reps' was not provided a number");
        }

        try {
            Integer.parseInt(data[4]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'sets' was not provided a number");
        }

        return data;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("add exercise");
        res.add("add the first exercise");
        res.add("/add_exercise");

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

