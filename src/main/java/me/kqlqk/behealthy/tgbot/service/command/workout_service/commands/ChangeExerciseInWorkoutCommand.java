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
public class ChangeExerciseInWorkoutCommand extends Command {
    private final SendMessage sendMessage;
    private final List<Object> sendObjects;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetWorkoutPlanCommand getWorkoutPlanCommand;

    @Autowired
    public ChangeExerciseInWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetWorkoutPlanCommand getWorkoutPlanCommand) {
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
            String text = "Enter the name of the exercise you want to change";
            sendMessage.setText(text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.CHANGE_EXERCISE_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_EXERCISE_WAIT_FOR_DATA) {
            try {
                gatewayClient.updateWorkoutWithAlternativeExercise(tgUser.getUserId(), userMessage, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage.setText(e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            SendMessage sendMessage1 = new SendMessage(chatId, "Successfully");

            getWorkoutPlanCommand.handle(update, tgUser, accessTokenDTO, securityState);
            SendMessage sendMessage2 = getWorkoutPlanCommand.getSendMessage();

            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("change exercises");
        res.add("/change_exercises");

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
