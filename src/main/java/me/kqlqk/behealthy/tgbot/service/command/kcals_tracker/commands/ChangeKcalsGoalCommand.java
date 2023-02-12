package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.OwnDailyKcalsDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
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
public class ChangeKcalsGoalCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public ChangeKcalsGoalCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage = new SendMessage(chatId, "Would you like to get your kilocalories goal from our system or set it yourself?");
            sendMessage.setReplyMarkup(initKeyboard());

            tgUser.setCommandSate(CommandState.CHANGE_KCALS_GOAL_WAIT_FOR_CHOOSING);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_KCALS_GOAL_WAIT_FOR_CHOOSING) {
            if (userMessage.equalsIgnoreCase("Do it for me")) {
                OwnDailyKcalsDTO ownDailyKcalsDTO = null;

                try {
                    ownDailyKcalsDTO = gatewayClient.getOwnDailyKcalsByUserId(tgUser.getUserId(), tokensDTO.getAccessToken());
                }
                catch (RuntimeException ignored) {
                }

                if (ownDailyKcalsDTO != null) {
                    ownDailyKcalsDTO.setInPriority(false);

                    try {
                        gatewayClient.changePriorityOwnDailyKcals(tgUser.getUserId(), ownDailyKcalsDTO, tokensDTO.getAccessToken());
                    }
                    catch (RuntimeException e) {
                        sendMessage = new SendMessage(chatId, e.getMessage());
                        sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

                        tgUser.setCommandSate(CommandState.BASIC);
                        telegramUserService.update(tgUser);
                        return;
                    }
                }

                UserConditionDTO userConditionDTO = null;
                try {
                    userConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), tokensDTO.getAccessToken());
                }
                catch (RuntimeException ignored) {
                }

                if (userConditionDTO == null) {
                    sendMessage = new SendMessage(chatId, "Please fill in your body condition " +
                            "so that we can generate kilocalories goal for you");
                }
                else {
                    sendMessage = new SendMessage(chatId, "Done. \n" +
                            "If you want to update it, you should update your body condition");
                }

                sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());
                tgUser.setCommandSate(CommandState.BASIC);
                telegramUserService.update(tgUser);
                return;
            }
            else if (userMessage.equalsIgnoreCase("I will set myself")) {
                String text = "Let's set your kilocalories goal." +
                        "\nUse the following pattern: proteins fats carbs";
                sendMessage = new SendMessage(chatId, text);
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

                tgUser.setCommandSate(CommandState.CHANGE_KCALS_GOAL_WAIT_FOR_DATA);
                telegramUserService.update(tgUser);
                return;
            }
        }
        else if (tgUser.getCommandSate() == CommandState.CHANGE_KCALS_GOAL_WAIT_FOR_DATA) {
            String[] dailyKcals;

            try {
                dailyKcals = splitDailyKcals(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            OwnDailyKcalsDTO ownDailyKcalsDTO = null;
            try {
                ownDailyKcalsDTO = gatewayClient.getOwnDailyKcalsByUserId(tgUser.getUserId(), tokensDTO.getAccessToken());
            }
            catch (RuntimeException ignored) {
            }

            if (ownDailyKcalsDTO == null) {
                ownDailyKcalsDTO = new OwnDailyKcalsDTO();
                ownDailyKcalsDTO.setProtein(Integer.parseInt(dailyKcals[0]));
                ownDailyKcalsDTO.setFat(Integer.parseInt(dailyKcals[1]));
                ownDailyKcalsDTO.setCarb(Integer.parseInt(dailyKcals[2]));
                ownDailyKcalsDTO.setInPriority(true);

                try {
                    gatewayClient.createOwnDailyKcals(tgUser.getUserId(), ownDailyKcalsDTO, tokensDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    sendMessage = new SendMessage(chatId, e.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }
            }
            else {
                ownDailyKcalsDTO.setProtein(Integer.parseInt(dailyKcals[0]));
                ownDailyKcalsDTO.setFat(Integer.parseInt(dailyKcals[1]));
                ownDailyKcalsDTO.setCarb(Integer.parseInt(dailyKcals[2]));
                ownDailyKcalsDTO.setInPriority(true);

                try {
                    gatewayClient.updateOwnDailyKcals(tgUser.getUserId(), ownDailyKcalsDTO, tokensDTO.getAccessToken());
                }
                catch (RuntimeException e) {
                    sendMessage = new SendMessage(chatId, e.getMessage());
                    sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                    return;
                }
            }

            sendMessage = new SendMessage(chatId, "Successfully added");
            sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            return;
        }

        sendMessage = null;
    }

    private ReplyKeyboardMarkup initKeyboard() {
        ReplyKeyboardMarkup keyboard = new ReplyKeyboardMarkup();
        List<KeyboardRow> keyboardRows = new ArrayList<>();

        KeyboardRow keyboardRow = new KeyboardRow();
        keyboardRow.add("Do it for me");
        keyboardRow.add("I will set myself");
        keyboardRows.add(keyboardRow);

        keyboardRow = new KeyboardRow();
        keyboardRow.add("Back ↩");
        keyboardRows.add(keyboardRow);

        keyboard.setKeyboard(keyboardRows);
        keyboard.setResizeKeyboard(true);
        return keyboard;
    }

    private String[] splitDailyKcals(String data) {
        String[] split = data.split(" ");

        if (split.length < 3) {
            throw new BadUserDataException("Please, use the following pattern: proteins fats carbs");
        }

        try {
            Integer.parseInt(split[0]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'proteins' was not provided a number");
        }

        try {
            Integer.parseInt(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'fats' was not provided a number");
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'carbs' was not provided a number");
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/change_kcals_goal");
        res.add("change kilocalories goal ⚙");
        res.add("change kilocalories goal");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
