package me.kqlqk.behealthy.tgbot.service.command.commands.user.workout_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_dto.ExerciseDTO;
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
public class GetExercisesByMuscleGroupCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetExercisesByMuscleGroupCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage = new SendMessage(chatId, "Enter muscle group to get exercises for it.");

            tgUser.setCommandSate(CommandState.GET_EXERCISES_BY_MUSCLE_GROUP_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.GET_EXERCISES_BY_MUSCLE_GROUP_WAIT_FOR_DATA) {
            List<ExerciseDTO> exerciseDTOs;

            try {
                exerciseDTOs = gatewayClient.getExercisesByMuscleGroup(tgUser.getUserId(), userMessage, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            StringBuilder text = new StringBuilder();

            for (ExerciseDTO exerciseDTO : exerciseDTOs) {
                text.append("Name: " + exerciseDTO.getName());
                text.append("\nDescription: " + exerciseDTO.getDescription());
                text.append("\nMuscle group: " + exerciseDTO.getMuscleGroup() + "\n");
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(chatId, text.toString());
            return;
        }

        sendMessage = null;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
