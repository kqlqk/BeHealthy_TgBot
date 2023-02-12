package me.kqlqk.behealthy.tgbot.service.command.commands.user.workout_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
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
public class UpdateWorkoutByAlternativeExerciseCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public UpdateWorkoutByAlternativeExerciseCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            String text = "Let's update your workout plan by alternative exercise." +
                    "\nEnter name of exercise that you want to change";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.UPDATE_WITH_ALTERNATIVE_EXERCISE_WORKOUT_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.UPDATE_WITH_ALTERNATIVE_EXERCISE_WORKOUT_WAIT_FOR_DATA) {
            try {
                gatewayClient.updateWorkoutWithAlternativeExercise(tgUser.getUserId(), userMessage, tokensDTO.getAccessToken());
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
