package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.DailyAteFoodDTO;
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

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class AddFoodCommand implements Command {
    private List<SendMessage> sendMessages;
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetFoodCommand getFoodCommand;

    @Autowired
    public AddFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetFoodCommand getFoodCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.getFoodCommand = getFoodCommand;
        sendMessages = new ArrayList<>();
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
            String text = "Let's add food that you are going to eat or have already eaten." +
                    "\nUse the following pattern: name weight(in g.) proteins(per 100 g.) fats(per 100 g.) carbs(per 100 g.)";
            sendMessage = new SendMessage(chatId, text);
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.ADD_FOOD_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
            return;
        }
        else if (tgUser.getCommandSate() == CommandState.ADD_FOOD_WAIT_FOR_DATA) {
            String[] food;

            try {
                food = splitFood(userMessage);
            }
            catch (BadUserDataException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            DailyAteFoodDTO dailyAteFoodDTO = new DailyAteFoodDTO(
                    food[0],
                    Double.parseDouble(food[1]),
                    Double.parseDouble(food[2]),
                    Double.parseDouble(food[3]),
                    Double.parseDouble(food[4]));

            try {
                gatewayClient.addDailyAteFoods(tgUser.getUserId(), dailyAteFoodDTO, tokensDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);

            SendMessage sendMessage1 = new SendMessage(chatId, "Food was successfully added");
            sendMessage1.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            getFoodCommand.handle(update, tgUser, tokensDTO, securityState);
            SendMessage sendMessage2 = getFoodCommand.getSendMessage();
            sendMessage2.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            sendMessages.add(sendMessage1);
            sendMessages.add(sendMessage2);
            return;
        }

        sendMessage = null;
        sendMessages = null;
    }

    private String[] splitFood(String data) {
        String[] split = data.split(" ");

        if (split.length < 5) {
            throw new BadUserDataException("Please, use the following pattern: name weight(in g.) proteins(per 100 g.) fats(per 100 g.) carbs(per 100 g.)");
        }

        try {
            Double.parseDouble(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Double.parseDouble(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'proteins' was not provided a number");
        }

        try {
            Double.parseDouble(split[3]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'fats' was not provided a number");
        }

        try {
            Double.parseDouble(split[4]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'carbs' was not provided a number");
        }

        return split;
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/add_food");
        res.add("add food ➕");
        res.add("add food");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }

    @Override
    public SendMessage[] getSendMessages() {
        return sendMessages.toArray(new SendMessage[sendMessages.size()]);
    }
}
