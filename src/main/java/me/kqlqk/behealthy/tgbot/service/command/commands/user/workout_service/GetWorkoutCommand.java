package me.kqlqk.behealthy.tgbot.service.command.commands.user.workout_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_dto.WorkoutInfoDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.List;

@Service
public class GetWorkoutCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        List<WorkoutInfoDTO> workout;

        try {
            workout = gatewayClient.getWorkout(tgUser.getUserId(), tokensDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            sendMessage = new SendMessage(chatId, e.getMessage());
            return;
        }

        StringBuilder text = new StringBuilder("Your current workout plan:\n\n");

        WorkoutInfoDTO oldWorkoutInfoDTO = workout.get(0);
        for (WorkoutInfoDTO workoutInfoDTO : workout) {
            if (workoutInfoDTO.getDay() != oldWorkoutInfoDTO.getDay()) {
                text.append("Day " + workoutInfoDTO.getDay() + ":\n");
            }
            if (workoutInfoDTO.getDay() == 1 && workoutInfoDTO.getNumberPerDay() == 1) {
                text.append("Day " + workoutInfoDTO.getDay() + ":\n");
            }

            text.append(workoutInfoDTO.getExercise().getName() + ": " + workoutInfoDTO.getReps() + "x" + workoutInfoDTO.getSets() + "\n");

            oldWorkoutInfoDTO = workoutInfoDTO;
        }

        sendMessage = new SendMessage(chatId, text.toString());
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
