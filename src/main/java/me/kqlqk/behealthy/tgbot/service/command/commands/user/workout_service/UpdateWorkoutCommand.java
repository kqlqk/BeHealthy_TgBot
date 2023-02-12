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

@Service
public class UpdateWorkoutCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public UpdateWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            String text = "Let's update your workout plan.\nEnter how many times per week you can go to the gym (from 1 to 5)";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.UPDATE_WORKOUT_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.UPDATE_WORKOUT_WAIT_FOR_DATA) {
            int workoutsPerWeek;
            try {
                workoutsPerWeek = Integer.parseInt(userMessage);
            }
            catch (NumberFormatException e) {
                sendMessage = new SendMessage(chatId, "Please, use a valid number");
                return;
            }

            WorkoutInfoDTO workoutInfoDTO = new WorkoutInfoDTO();
            workoutInfoDTO.setWorkoutsPerWeek(workoutsPerWeek);

            try {
                gatewayClient.updateWorkout(tgUser.getUserId(), workoutInfoDTO, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(chatId, "Workout plan was successfully updated");
            return;
        }

        sendMessage = null;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
