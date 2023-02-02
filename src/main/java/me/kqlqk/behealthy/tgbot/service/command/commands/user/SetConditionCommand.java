package me.kqlqk.behealthy.tgbot.service.command.commands.user;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.authService.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionDTO;
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
public class SetConditionCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public SetConditionCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
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
            String text = "Enter your condition. \nUse the following pattern: gender age height weight intensity goal fatPercent";
            sendMessage = new SendMessage(chatId, text);

            tgUser.setCommandSate(CommandState.SET_CONDITION_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.SET_CONDITION_WAIT_FOR_DATA) {
            String[] condition;

            try {
                condition = splitCondition(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            UserConditionDTO userConditionDTO = new UserConditionDTO(condition[0],
                                                                     Integer.parseInt(condition[1]),
                                                                     Integer.parseInt(condition[2]),
                                                                     Integer.parseInt(condition[3]),
                                                                     condition[4],
                                                                     condition[5],
                                                                     Double.parseDouble(condition[6]));

            try {
                gatewayClient.createUserCondition(tgUser.getUserId(), userConditionDTO, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            sendMessage = new SendMessage(chatId, "Condition was successfully created");
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
            throw new BadUserDataException("For 'age' was not provided a number");
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'height' was not provided a number");
        }

        try {
            Integer.parseInt(split[3]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Double.parseDouble(split[6]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'fatPercent' was not provided a number");
        }

        return split;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
