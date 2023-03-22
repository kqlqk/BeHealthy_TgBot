package me.kqlqk.behealthy.tgbot.service.command.workout_service.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
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
public class RemoveExerciseFromUserWorkoutCommand extends Command {
    private SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetUserWorkoutCommand getUserWorkoutCommand;

    @Autowired
    public RemoveExerciseFromUserWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetUserWorkoutCommand getUserWorkoutCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.getUserWorkoutCommand = getUserWorkoutCommand;
        sendObjects = new ArrayList<>();
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

        if (tgUser.getCommandSate() == CommandState.BASIC || tgUser.getCommandSate() == CommandState.RETURN_TO_WORKOUT_SERVICE_MENU) {
            String text = "Let's remove exercise from your plan\n" +
                    "Enter the name of exercise";
            sendMessage = new SendMessage(chatId, text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.REMOVE_EXERCISE_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.REMOVE_EXERCISE_WAIT_FOR_DATA) {
            try {
                gatewayClient.removeExercise(tgUser.getUserId(), userMessage, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            getUserWorkoutCommand.handle(update, tgUser, accessTokenDTO, securityState);

            SendMessage sendMessage1 = new SendMessage(chatId, "Successfully");
            SendMessage sendMessage2 = getUserWorkoutCommand.getSendMessage();

            try {
                gatewayClient.getUserWorkout(tgUser.getUserId(), accessTokenDTO.getAccessToken());
                sendMessage2.setReplyMarkup(GetUserWorkoutCommand.initKeyboard());
            }
            catch (RuntimeException e) {
                if (!e.getMessage().equals("Your own workout not found")) {
                    log.error("Something went wrong", e);
                }

                sendMessage2.setReplyMarkup(GetUserWorkoutCommand.add1stExerciseKeyboard());
            }

            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("remove exercise");
        res.add("/remove_exercise");

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

