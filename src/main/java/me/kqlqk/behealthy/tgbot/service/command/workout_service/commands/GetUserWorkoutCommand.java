package me.kqlqk.behealthy.tgbot.service.command.workout_service.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_service.GetUserWorkoutDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.ReplyKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.KeyboardRow;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Scope("prototype")
@Slf4j
public class GetUserWorkoutCommand extends Command {
    private final SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetUserWorkoutCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        sendMessage = new SendMessage();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        sendMessage.setChatId(update.getMessage().getChatId().toString());

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage.setText("Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        tgUser.setCommandSate(CommandState.RETURN_TO_WORKOUT_SERVICE_MENU);
        telegramUserService.update(tgUser);

        List<GetUserWorkoutDTO> workout;

        try {
            workout = gatewayClient.getUserWorkout(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            if (!e.getMessage().equals("Your own workout not found")) {
                log.error("Something went wrong", e);

                sendMessage.setText(e.getMessage());
                return;
            }

            sendMessage.setText("Looks like you don't have any exercises in your workout plan.\n" +
                                        "Add the first!");
            sendMessage.setReplyMarkup(add1stExerciseKeyboard());

            return;
        }

        sendMessage.setText(generateText(workout));
        sendMessage.enableHtml(true);
        sendMessage.setReplyMarkup(initKeyboard());
    }

    public static ReplyKeyboardMarkup add1stExerciseKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Add the first exercise");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    public static ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Add exercise");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Remove exercise");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String generateText(List<GetUserWorkoutDTO> getUserWorkoutDTOS) {
        List<GetUserWorkoutDTO> workout = getUserWorkoutDTOS.stream()
                .sorted(Comparator.comparingInt(GetUserWorkoutDTO::getDay).thenComparingInt(GetUserWorkoutDTO::getNumberPerDay))
                .collect(Collectors.toList());

        StringBuilder text = new StringBuilder("<b>Your workout plan:</b>\n");

        for (int i = 0; i < workout.size(); i++) {
            if (i == 0) {
                text.append("Day 1:\n");
            }

            GetUserWorkoutDTO current = workout.get(i);
            if (i > 0 && current.getDay() != workout.get(i - 1).getDay()) {
                text.append("Day ");
                text.append(current.getDay());
                text.append(":\n");
            }

            text.append("<pre>");
            text.append(current.getNumberPerDay());
            text.append(". ");
            text.append(StringUtils.capitalize(current.getExerciseName().toLowerCase()));
            text.append("\n[");
            text.append(current.getRep());
            text.append(" reps X ");
            text.append(current.getSet());
            text.append(" sets] </pre>\n\n");
        }

        return text.toString();
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("my own workout plan \uD83D\uDCA2");
        res.add("my own workout plan");
        res.add("/get_my_workout");

        return res;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
