package me.kqlqk.behealthy.tgbot.service.command.commands.user.condition_service;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateConditionCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public UpdateConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            String text = "Enter your condition (If you don't want to change specific param, use '-'). " +
                    "\nUse the following pattern: gender age height weight intensity goal fatPercent";

            sendMessage = new SendMessage(chatId, text);
            tgUser.setCommandSate(CommandState.UPDATE_CONDITION_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.UPDATE_CONDITION_WAIT_FOR_DATA) {
            String[] condition;

            try {
                condition = splitCondition(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            UserConditionDTO userConditionDTO;
            try {
                userConditionDTO = gatewayClient.getUserCondition(tgUser.getUserId(), tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            userConditionDTO.setGender(!condition[0].equals("-") ? condition[0] : userConditionDTO.getGender());
            userConditionDTO.setAge(!condition[1].equals("-") ? Integer.parseInt(condition[1]) : userConditionDTO.getAge());
            userConditionDTO.setHeight(!condition[2].equals("-") ? Integer.parseInt(condition[2]) : userConditionDTO.getHeight());
            userConditionDTO.setWeight(!condition[3].equals("-") ? Integer.parseInt(condition[3]) : userConditionDTO.getWeight());
            userConditionDTO.setIntensity(!condition[4].equals("-") ? condition[4] : userConditionDTO.getIntensity());
            userConditionDTO.setGoal(!condition[5].equals("-") ? condition[5] : userConditionDTO.getGoal());
            userConditionDTO.setFatPercent(!condition[6].equals("-") ? Double.parseDouble(condition[6]) : userConditionDTO.getFatPercent());

            try {
                gatewayClient.updateUserCondition(tgUser.getUserId(), userConditionDTO, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(chatId, "Condition was successfully updated");
            return;
        }

        sendMessage = null;
    }

    private String[] splitCondition(String data) {
        String[] split = data.split(" ");

        if (split.length < 7) {
            throw new BadUserDataException("Please, use the following pattern: gender age height weight intensity goal fatPercent");
        }

        try {
            Integer.parseInt(split[1]);
        }
        catch (NumberFormatException e) {
            if (!split[1].equals("-")) {
                throw new BadUserDataException("For 'age' was not provided a number");
            }
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            if (!split[1].equals("-")) {
                throw new BadUserDataException("For 'height' was not provided a number");
            }
        }

        try {
            Integer.parseInt(split[3]);
        }
        catch (NumberFormatException e) {
            if (!split[1].equals("-")) {
                throw new BadUserDataException("For 'weight' was not provided a number");
            }
        }

        try {
            Double.parseDouble(split[6]);
        }
        catch (NumberFormatException e) {
            if (!split[1].equals("-")) {
                throw new BadUserDataException("For 'fatPercent' was not provided a number");
            }
        }

        return split;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
