package me.kqlqk.behealthy.tgbot.service.command.workout_service.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.workout_service.GetWorkoutInfoDTO;
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
public class GetWorkoutPlanCommand extends Command {
    private final SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public GetWorkoutPlanCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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

        List<GetWorkoutInfoDTO> workout;

        try {
            workout = gatewayClient.getWorkoutInfos(tgUser.getUserId(), accessTokenDTO.getAccessToken());
        }
        catch (RuntimeException e) {
            if (!e.getMessage().equals("Workout not found")) {
                log.error("Something went wrong", e);

                sendMessage.setText(e.getMessage());
                return;
            }

            sendMessage.setText("Please, set workout");
            sendMessage.setReplyMarkup(setWorkoutKeyboard());

            return;
        }

        sendMessage.setText(generateText(workout));
        sendMessage.enableHtml(true);
        sendMessage.setReplyMarkup(initKeyboard());
    }

    private static ReplyKeyboardMarkup setWorkoutKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Set workout");
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
        keyboardRow.add("Change workout");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Change exercises");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String generateText(List<GetWorkoutInfoDTO> getWorkoutInfoDTOs) {
        List<GetWorkoutInfoDTO> workout = getWorkoutInfoDTOs.stream()
                .sorted(Comparator.comparingInt(GetWorkoutInfoDTO::getDay).thenComparingInt(GetWorkoutInfoDTO::getNumberPerDay))
                .collect(Collectors.toList());

        StringBuilder text = new StringBuilder("<b>Your workout plan:</b>\n");

        for (int i = 0; i < workout.size(); i++) {
            if (i == 0) {
                text.append("Day 1:\n");
            }

            GetWorkoutInfoDTO current = workout.get(i);
            if (i > 0 && current.getDay() != workout.get(i - 1).getDay()) {
                text.append("Day ");
                text.append(current.getDay());
                text.append(":\n");
            }

            text.append("<pre>");
            text.append(current.getNumberPerDay());
            text.append(". ");
            text.append(StringUtils.capitalize(current.getExercise().getName().toLowerCase()));
            text.append(current.getExercise().getAlternativeId() != null ? " | Supports change \n[" : "\n[");
            text.append(current.getRep());
            text.append(" reps X ");
            text.append(current.getSet());
            text.append(" sets] </pre>\n\n");
        }

        return text.toString();
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("our workout plan for you \uD83D\uDCAB");
        res.add("our workout plan for you");
        res.add("/get_workout");

        return res;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
