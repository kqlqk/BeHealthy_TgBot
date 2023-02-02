package me.kqlqk.behealthy.tgbot.service.command.commands.user;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.authService.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionWithoutFatPercentFemaleDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionWithoutFatPercentMaleDTO;
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
public class SetConditionNoFatPercentCommand implements Command {
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public SetConditionNoFatPercentCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, TokensDTO tokensDTO, SecurityState securityState) {
        String chatId = update.getMessage().getChatId().toString();
        String userMessage = update.getMessage().getText();
        String text;

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            sendMessage = new SendMessage(chatId, "Enter your condition: \n" + "First of all choose your gender");

            tgUser.setCommandSate(CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_GENDER);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_GENDER) {
            if (userMessage.equalsIgnoreCase("male")) {
                tgUser.setCommandSate(CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_MALE);
                telegramUserService.update(tgUser);

                text = "Now use the following pattern: \n" +
                        "age height weight intensity goal" + " 'Fat fold between chest and ilium'" +
                        " 'Fat fold between navel and lower belly' 'Fat fold between nipple and armpit'" +
                        " 'Fat fold between nipple and upper chest'";
            }
            else {
                tgUser.setCommandSate(CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_FEMALE);
                telegramUserService.update(tgUser);

                text = "Now use the following pattern: \n" +
                        "age height weight intensity goal" + " 'Fat fold between shoulder and elbow'" +
                        " 'Fat fold between chest and lower ilium' 'Fat fold between navel and lower belly'";
            }

            sendMessage = new SendMessage(chatId, text);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_MALE) {
            String[] maleCondition;

            try {
                maleCondition = splitMaleCondition(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            UserConditionWithoutFatPercentMaleDTO condition = new UserConditionWithoutFatPercentMaleDTO(
                    Integer.parseInt(maleCondition[0]),
                    Integer.parseInt(maleCondition[1]),
                    Integer.parseInt(maleCondition[2]),
                    maleCondition[3],
                    maleCondition[4],
                    Integer.parseInt(maleCondition[5]),
                    Integer.parseInt(maleCondition[6]),
                    Integer.parseInt(maleCondition[7]),
                    Integer.parseInt(maleCondition[8]));

            try {
                gatewayClient.createUserConditionWithoutFatPercentMale(tgUser.getUserId(), condition, tokensDTO.getAccessToken());
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
        else if (tgUser.getCommandSate() == CommandState.SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_FEMALE) {
            String[] femaleCondition;

            try {
                femaleCondition = splitFemaleCondition(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                return;
            }

            UserConditionWithoutFatPercentFemaleDTO condition = new UserConditionWithoutFatPercentFemaleDTO(
                    Integer.parseInt(femaleCondition[0]),
                    Integer.parseInt(femaleCondition[1]),
                    Integer.parseInt(femaleCondition[2]),
                    femaleCondition[3],
                    femaleCondition[4],
                    Integer.parseInt(femaleCondition[5]),
                    Integer.parseInt(femaleCondition[6]),
                    Integer.parseInt(femaleCondition[7]));

            try {
                gatewayClient.createUserConditionWithoutFatPercentFemale(tgUser.getUserId(), condition, tokensDTO.getAccessToken());
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


    private String[] splitMaleCondition(String data) {
        String[] split = data.split(" ");

        if (split.length < 9) {
            throw new BadUserDataException("Please, use the following pattern: gender age height weight intensity goal fatPercent");
        }

        try {
            Integer.parseInt(split[0]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'age' was not provided a number");
        }

        try {
            Integer.parseInt(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'height' was not provided a number");
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Integer.parseInt(split[5]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between chest and ilium' was not provided a number");
        }

        try {
            Integer.parseInt(split[6]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between navel and lower belly' was not provided a number");
        }

        try {
            Integer.parseInt(split[7]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between nipple and armpit' was not provided a number");
        }

        try {
            Integer.parseInt(split[8]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between nipple and upper chest' was not provided a number");
        }

        return split;
    }

    private String[] splitFemaleCondition(String data) {
        String[] split = data.split(" ");

        if (split.length < 8) {
            throw new BadUserDataException("Please, use the following pattern: gender age height weight intensity goal fatPercent");
        }

        try {
            Integer.parseInt(split[0]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'age' was not provided a number");
        }

        try {
            Integer.parseInt(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'height' was not provided a number");
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Integer.parseInt(split[5]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between shoulder and elbow' was not provided a number");
        }

        try {
            Integer.parseInt(split[6]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between chest and ilium' was not provided a number");
        }

        try {
            Integer.parseInt(split[7]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'Fat fold between navel and lower belly' was not provided a number");
        }

        return split;
    }


    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
