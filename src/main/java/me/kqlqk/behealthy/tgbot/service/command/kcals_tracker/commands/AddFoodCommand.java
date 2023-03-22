package me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands;

import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.AddDailyAteFoodDTO;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.GetDailyAteFoodDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.BackCommand;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
import me.kqlqk.behealthy.tgbot.util.Maps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.InlineKeyboardMarkup;
import org.telegram.telegrambots.meta.api.objects.replykeyboard.buttons.InlineKeyboardButton;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class AddFoodCommand extends Command {
    private final List<Object> sendObjects;
    private SendMessage sendMessage;

    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;
    private final GetFoodCommand getFoodCommand;

    @Autowired
    public AddFoodCommand(TelegramUserService telegramUserService, GatewayClient gatewayClient, GetFoodCommand getFoodCommand) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
        this.getFoodCommand = getFoodCommand;
        sendObjects = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
        String chatId;
        String userMessage = null;
        if (update.hasCallbackQuery()) {
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        }
        else {
            chatId = update.getMessage().getChatId().toString();
            userMessage = update.getMessage().getText();
        }

        if (securityState == SecurityState.SHOULD_RELOGIN) {
            sendMessage = new SendMessage(chatId, "Sorry, you should sign in again");
            sendMessage.setReplyMarkup(defaultKeyboard(false));

            tgUser.setCommandSate(CommandState.BASIC);
            tgUser.setActive(false);

            telegramUserService.update(tgUser);
            return;
        }

        if (update.hasCallbackQuery()) {
            handleCallBackQuery(update, tgUser, accessTokenDTO);
            return;
        }

        if (tgUser.getCommandSate() == CommandState.BASIC) {
            String text = "Let's add food that you are going to eat or have already eaten." +
                    "\nUse the following pattern: name weight(in g.) protein(per 100 g.) fat(per 100 g.) carb(per 100 g.)";
            SendMessage sendMessage1 = new SendMessage(chatId, text);
            sendMessage1.setReplyMarkup(onlyBackCommandKeyboard());

            SendMessage sendMessage2 = new SendMessage(chatId, "Food that you have already added:");
            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            List<GetDailyAteFoodDTO> getDailyAteFoodDTOs;
            try {
                getDailyAteFoodDTOs = gatewayClient.getAllDailyAteFoods(tgUser.getUserId(), accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            for (int i = 0; i < getDailyAteFoodDTOs.size(); i++) {
                if (i > 5) {
                    break;
                }

                List<InlineKeyboardButton> row = new ArrayList<>();
                InlineKeyboardButton button = new InlineKeyboardButton(getDailyAteFoodDTOs.get(i).getName());
                button.setCallbackData("AddFoodCommand_" + getDailyAteFoodDTOs.get(i).getName());
                row.add(button);
                rows.add(row);
            }

            if (getDailyAteFoodDTOs.size() > 5) {
                List<InlineKeyboardButton> row2 = new ArrayList<>();
                InlineKeyboardButton buttonNext = new InlineKeyboardButton("\t➡");
                buttonNext.setCallbackData("AddFoodCommand_next");
                row2.add(buttonNext);
                rows.add(row2);
            }

            inlineKeyboardMarkup.setKeyboard(rows);
            sendMessage2.setReplyMarkup(inlineKeyboardMarkup);

            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);

            tgUser.setCommandSate(CommandState.ADD_FOOD_WAIT_FOR_DATA);
            telegramUserService.update(tgUser);
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

            AddDailyAteFoodDTO addDailyAteFoodDTO = new AddDailyAteFoodDTO(food[0],
                                                                           Double.parseDouble(food[1]),
                                                                           Integer.parseInt(food[2]),
                                                                           Integer.parseInt(food[3]),
                                                                           Integer.parseInt(food[4]),
                                                                           true);

            try {
                gatewayClient.saveDailyAteFood(tgUser.getUserId(), addDailyAteFoodDTO, accessTokenDTO.getAccessToken());
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

            getFoodCommand.handle(update, tgUser, accessTokenDTO, securityState);
            SendMessage sendMessage2 = getFoodCommand.getSendMessage();
            sendMessage2.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
        else if (tgUser.getCommandSate() == CommandState.ADD_FOOD_WAIT_FOR_DATA_CALLBACK) {
            String name = Maps.getUserIdAddFoodCallback(tgUser.getUserId()).substring(15);
            AddDailyAteFoodDTO addDailyAteFoodDTO = new AddDailyAteFoodDTO();
            addDailyAteFoodDTO.setName(name);
            addDailyAteFoodDTO.setToday(true);
            GetDailyAteFoodDTO getDailyAteFoodDTO;

            try {
                getDailyAteFoodDTO = gatewayClient.getDailyAteFoods(tgUser.getUserId(), name, accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            addDailyAteFoodDTO.setProtein(getDailyAteFoodDTO.getProtein());
            addDailyAteFoodDTO.setFat(getDailyAteFoodDTO.getFat());
            addDailyAteFoodDTO.setCarb(getDailyAteFoodDTO.getCarb());

            Double weight;
            try {
                weight = splitWeight(userMessage);
            }
            catch (BadUserDataException e) {
                SendMessage sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                sendObjects.add(sendMessage);
                return;
            }

            Maps.removeUserIdAddFoodCallback(tgUser.getUserId());

            addDailyAteFoodDTO.setWeight(weight);

            try {
                gatewayClient.updateDailyAteFood(tgUser.getUserId(), addDailyAteFoodDTO, accessTokenDTO.getAccessToken());
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

            getFoodCommand.handle(update, tgUser, accessTokenDTO, securityState);
            SendMessage sendMessage2 = getFoodCommand.getSendMessage();
            sendMessage2.setReplyMarkup(KcalsTrackerMenu.initKeyboard());

            sendObjects.add(sendMessage1);
            sendObjects.add(sendMessage2);
        }
    }

    private void handleCallBackQuery(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO) {
        String chatId = update.getCallbackQuery().getMessage().getChatId().toString();

        BackCommand.setInfoToDeleteMessage(chatId, update.getCallbackQuery().getMessage().getMessageId());

        if (update.getCallbackQuery().getData().equalsIgnoreCase("AddFoodCommand_next") ||
                update.getCallbackQuery().getData().equalsIgnoreCase("AddFoodCommand_previous")) {
            boolean nextButton = update.getCallbackQuery().getData().equalsIgnoreCase("AddFoodCommand_next");

            List<GetDailyAteFoodDTO> getDailyAteFoodDTOs;
            try {
                getDailyAteFoodDTOs = gatewayClient.getAllDailyAteFoods(tgUser.getUserId(), accessTokenDTO.getAccessToken());
            }
            catch (RuntimeException e) {
                sendMessage = new SendMessage(chatId, e.getMessage());
                sendMessage.setReplyMarkup(onlyBackCommandKeyboard());
                return;
            }

            int pages = getDailyAteFoodDTOs.size() % 5 == 0 ? getDailyAteFoodDTOs.size() / 5 : getDailyAteFoodDTOs.size() / 5 + 1;
            int currentPage = Maps.getUserIdPage(tgUser.getUserId()) == null ? 1 : Maps.getUserIdPage(tgUser.getUserId());
            int pageToGo = nextButton ? currentPage + 1 : currentPage - 1;

            Maps.putUserIdPage(tgUser.getUserId(), pageToGo);

            InlineKeyboardMarkup inlineKeyboardMarkup = new InlineKeyboardMarkup();
            List<List<InlineKeyboardButton>> rows = new ArrayList<>();

            for (int i = 0; i < getDailyAteFoodDTOs.size(); i++) {
                if (nextButton) {
                    if (i >= currentPage * 5 && i < pageToGo * 5) {
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        InlineKeyboardButton button = new InlineKeyboardButton(getDailyAteFoodDTOs.get(i).getName());
                        button.setCallbackData("AddFoodCommand_" + getDailyAteFoodDTOs.get(i).getName());
                        row.add(button);
                        rows.add(row);
                    }
                }
                else {
                    if (i >= (pageToGo - 1) * 5 && i < (currentPage - 1) * 5) {
                        List<InlineKeyboardButton> row = new ArrayList<>();
                        InlineKeyboardButton button = new InlineKeyboardButton(getDailyAteFoodDTOs.get(i).getName());
                        button.setCallbackData("AddFoodCommand_" + getDailyAteFoodDTOs.get(i).getName());
                        row.add(button);
                        rows.add(row);
                    }
                }
            }

            List<InlineKeyboardButton> row2 = new ArrayList<>();
            if (pageToGo > 1) {
                InlineKeyboardButton buttonNext = new InlineKeyboardButton("⬅");
                buttonNext.setCallbackData("AddFoodCommand_previous");
                row2.add(buttonNext);
            }
            if (pageToGo < pages) {
                InlineKeyboardButton buttonNext = new InlineKeyboardButton("\t➡");
                buttonNext.setCallbackData("AddFoodCommand_next");
                row2.add(buttonNext);
            }
            rows.add(row2);

            inlineKeyboardMarkup.setKeyboard(rows);

            EditMessageText editMessageText = new EditMessageText();
            editMessageText.setChatId(chatId);
            editMessageText.setText("Food that you have already added: (page: " + pageToGo + " / " + pages + ")");
            editMessageText.setMessageId(update.getCallbackQuery().getMessage().getMessageId());
            editMessageText.setReplyMarkup(inlineKeyboardMarkup);
            sendObjects.add(editMessageText);
        }
        else {
            sendMessage = new SendMessage(chatId, "Enter weight of the product");
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            Maps.putUserIdAddFoodCallback(tgUser.getUserId(), update.getCallbackQuery().getData());

            tgUser.setCommandSate(CommandState.ADD_FOOD_WAIT_FOR_DATA_CALLBACK);
            telegramUserService.update(tgUser);
        }
    }

    private Double splitWeight(String data) {
        double res;
        try {
            res = Double.parseDouble(data);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        return res;
    }

    private String[] splitFood(String data) {
        String[] split = data.split(" ");

        if (split.length < 5) {
            throw new BadUserDataException("Please, use the following pattern: name weight(in g.) protein(per 100 g.) fat(per 100 g.) carb(per 100 g.)");
        }

        try {
            Double.parseDouble(split[1]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'weight' was not provided a number");
        }

        try {
            Integer.parseInt(split[2]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'protein' was not provided a number");
        }

        try {
            Integer.parseInt(split[3]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'fat' was not provided a number");
        }

        try {
            Integer.parseInt(split[4]);
        }
        catch (NumberFormatException e) {
            throw new BadUserDataException("For 'carb' was not provided a number");
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
    public Object[] getSendObjects() {
        return sendObjects.toArray(new Object[sendObjects.size()]);
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
