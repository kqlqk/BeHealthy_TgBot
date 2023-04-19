package me.kqlqk.behealthy.tgbot.service.impl;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.ValidateDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.RefreshTokenDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.ChatId;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.ChatIdService;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.UpdateService;
import me.kqlqk.behealthy.tgbot.service.command.BackCommand;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.admin_menu.AdminMenu;
import me.kqlqk.behealthy.tgbot.service.command.admin_menu.commands.LogsCommand;
import me.kqlqk.behealthy.tgbot.service.command.admin_menu.commands.SendMessageCommand;
import me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.BodyConditionMenu;
import me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.commands.*;
import me.kqlqk.behealthy.tgbot.service.command.guest_menu.DefaultGuestCommand;
import me.kqlqk.behealthy.tgbot.service.command.guest_menu.LoginCommand;
import me.kqlqk.behealthy.tgbot.service.command.guest_menu.RegistrationCommand;
import me.kqlqk.behealthy.tgbot.service.command.guest_menu.StartCommand;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands.AddFoodCommand;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands.ChangeKcalGoalCommand;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.commands.GetFoodCommand;
import me.kqlqk.behealthy.tgbot.service.command.user_commands.MeCommand;
import me.kqlqk.behealthy.tgbot.service.command.workout_service.WorkoutServiceMenu;
import me.kqlqk.behealthy.tgbot.service.command.workout_service.commands.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
@Slf4j
public class UpdateServiceImpl implements UpdateService {
    private final TelegramUserService telegramUserService;
    private final ApplicationContext context;
    private Command command;
    private final GatewayClient gatewayClient;
    private final ChatIdService chatIdService;

    @Autowired
    public UpdateServiceImpl(TelegramUserService telegramUserService, ApplicationContext context, GatewayClient gatewayClient, ChatIdService chatIdService) {
        this.telegramUserService = telegramUserService;
        this.context = context;
        this.gatewayClient = gatewayClient;
        this.chatIdService = chatIdService;
    }

    @Override
    public Object handle(Update update) {
        long tgId;
        String chatId;

        if (!update.hasMessage() && !update.hasCallbackQuery()) {
            return null;
        }

        if (update.hasMessage()) {
            tgId = update.getMessage().getFrom().getId();
            chatId = update.getMessage().getChatId().toString();
        }
        else {
            tgId = update.getCallbackQuery().getFrom().getId();
            chatId = update.getCallbackQuery().getMessage().getChatId().toString();
        }

        if (!telegramUserService.existsByTelegramId(tgId)) {
            TelegramUser newTgUser = new TelegramUser();
            newTgUser.setTelegramId(tgId);
            newTgUser.setActive(false);
            newTgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.save(newTgUser);

            ChatId chatIdEntity = new ChatId();
            chatIdEntity.setChatId(chatId);
            chatIdService.save(chatIdEntity);
        }

        TelegramUser tgUser = telegramUserService.getByTelegramId(tgId);
        Command c = new Command() {
        };

        if (tgUser.isActive()) {
            ValidateDTO validateDTO;
            try {
                validateDTO = gatewayClient.validateRefreshToken(new RefreshTokenDTO(tgUser.getRefreshToken()));
            }
            catch (RuntimeException e) {
                validateDTO = new ValidateDTO();
                validateDTO.setValid(false);
            }

            if (!validateDTO.isValid()) {
                SendMessage sendMessage = new SendMessage(chatId, "Please sign in / sign up again");
                sendMessage.setReplyMarkup(c.defaultKeyboard(false));

                tgUser.setActive(false);
                tgUser.setCommandSate(CommandState.BASIC);
                telegramUserService.update(tgUser);

                return sendMessage;
            }
        }

        if (update.hasCallbackQuery()) {
            return handleCallbackQuery(update, tgUser);
        }

        if (BackCommand.getNames().contains(update.getMessage().getText().toLowerCase())) {
            return handleAndReturnSendObject(update, tgUser, "backCommand", BackCommand.class);
        }

        if (!tgUser.isActive()) {
            return choosingForInactiveUsers(update, tgUser);
        }

        if (update.getMessage().hasPhoto() && tgUser.getCommandSate() == CommandState.WAIT_FOR_PHOTO) {
            return handleAndReturnSendObjectSecured(update, tgUser, "addPhotoCommand", AddPhotoCommand.class);
        }

        if (!update.getMessage().hasText()) {
            return null;
        }

        if (tgUser.getTelegramId() == 538822850) {
            Object answer = choosingForAdmins(update, tgUser);
            if (answer != null) {
                return answer;
            }
        }

        Object answer = choosingBetweenCommandState(update, tgUser);
        return answer == null ?
                choosingBetweenCommands(update, tgUser) :
                answer;
    }

    private Object handleCallbackQuery(Update update, TelegramUser tgUser) {
        if (!update.hasCallbackQuery()) {
            return null;
        }

        if (update.getCallbackQuery().getData().startsWith("AddFoodCommand_")) {
            return handleAndReturnSendObjectSecured(update, tgUser, "addFoodCommand", AddFoodCommand.class);
        }

        if (update.getCallbackQuery().getData().startsWith("GetFoodCommand_")) {
            return handleAndReturnSendObjectSecured(update, tgUser, "getFoodCommand", GetFoodCommand.class);
        }

        return null;
    }

    private Object choosingForInactiveUsers(Update update, TelegramUser tgUser) {
        String userMessage = update.getMessage().getText().toLowerCase();

        switch (tgUser.getCommandSate()) {
            case LOGIN_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case REGISTRATION_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);
        }

        if (StartCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObject(update, tgUser, "startCommand", StartCommand.class);
        }
        else if (LoginCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);
        }
        else if (RegistrationCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);
        }
        else {
            return handleAndReturnSendObject(update, tgUser, "defaultGuestCommand", DefaultGuestCommand.class);
        }

    }

    private Object choosingBetweenCommands(Update update, TelegramUser tgUser) {
        String userMessage = update.getMessage().getText().toLowerCase();

        if (KcalsTrackerMenu.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "kcalsTrackerMenu", KcalsTrackerMenu.class);
        }

        if (GetFoodCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "getFoodCommand", GetFoodCommand.class);
        }

        if (AddFoodCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "addFoodCommand", AddFoodCommand.class);
        }

        if (ChangeKcalGoalCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "changeKcalGoalCommand", ChangeKcalGoalCommand.class);
        }


        if (BodyConditionMenu.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "bodyConditionMenu", BodyConditionMenu.class);
        }

        if (GetBodyConditionCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "getBodyConditionCommand", GetBodyConditionCommand.class);
        }

        if (SetBodyConditionCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "setBodyConditionCommand", SetBodyConditionCommand.class);
        }

        if (TrackChangesCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "trackChangesCommand", TrackChangesCommand.class);
        }

        if (AddPhotoCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "addPhotoCommand", AddPhotoCommand.class);
        }

        if (LoadOldPhotosCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "loadOldPhotosCommand", LoadOldPhotosCommand.class);
        }

        if (DeleteAllPhotosCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "deleteAllPhotosCommand", DeleteAllPhotosCommand.class);
        }


        if (WorkoutServiceMenu.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "workoutServiceMenu", WorkoutServiceMenu.class);
        }

        if (GetWorkoutPlanCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "getWorkoutPlanCommand", GetWorkoutPlanCommand.class);
        }

        if (SetWorkoutPlanCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "setWorkoutPlanCommand", SetWorkoutPlanCommand.class);
        }

        if (ChangeExerciseInWorkoutCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "changeExerciseInWorkoutCommand", ChangeExerciseInWorkoutCommand.class);
        }

        if (GetUserWorkoutCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "getUserWorkoutCommand", GetUserWorkoutCommand.class);
        }

        if (AddExerciseToUserWorkoutCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "addExerciseToUserWorkoutCommand", AddExerciseToUserWorkoutCommand.class);
        }

        if (RemoveExerciseFromUserWorkoutCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "removeExerciseFromUserWorkoutCommand", RemoveExerciseFromUserWorkoutCommand.class);
        }


        switch (update.getMessage().getText()) {
            case "/start":
                return handleAndReturnSendObject(update, tgUser, "startCommand", StartCommand.class);

            case "/login":
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case "/me":
                return handleAndReturnSendObjectSecured(update, tgUser, "meCommand", MeCommand.class);

            default:
                return null;
        }
    }

    private Object choosingBetweenCommandState(Update update, TelegramUser tgUser) {
        switch (tgUser.getCommandSate()) {
            case LOGIN_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case ADD_FOOD_WAIT_FOR_DATA:
            case ADD_FOOD_WAIT_FOR_DATA_CALLBACK:
                return handleAndReturnSendObjectSecured(update, tgUser, "addFoodCommand", AddFoodCommand.class);

            case CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING:
            case CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING_KCAL:
            case CHANGE_KCAL_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "changeKcalGoalCommand", ChangeKcalGoalCommand.class);

            case SET_BODY_CONDITION_WAIT_FOR_FAT_PERCENT:
            case SET_BODY_CONDITION_WAIT_FOR_GENDER:
            case SET_BODY_CONDITION_WAIT_FOR_ACTIVITY:
            case SET_BODY_CONDITION_WAIT_FOR_GOAL:
            case SET_BODY_CONDITION_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "setBodyConditionCommand", SetBodyConditionCommand.class);

            case SET_WORKOUT_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "setWorkoutPlanCommand", SetWorkoutPlanCommand.class);

            case CHANGE_EXERCISE_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "changeExerciseInWorkoutCommand", ChangeExerciseInWorkoutCommand.class);

            case ADD_EXERCISE_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "addExerciseToUserWorkoutCommand", AddExerciseToUserWorkoutCommand.class);

            case REMOVE_EXERCISE_WAIT_FOR_DATA:
                return handleAndReturnSendObjectSecured(update, tgUser, "removeExerciseFromUserWorkoutCommand", RemoveExerciseFromUserWorkoutCommand.class);

            case WAIT_FOR_PHOTO:
                return handleAndReturnSendObjectSecured(update, tgUser, "addPhotoCommand", AddPhotoCommand.class);

            case DELETE_ALL_PHOTOS_WAIT_FOD_CHOOSING:
                return handleAndReturnSendObjectSecured(update, tgUser, "deleteAllPhotosCommand", DeleteAllPhotosCommand.class);
        }

        return null;
    }

    private Object choosingForAdmins(Update update, TelegramUser tgUser) {
        String userMessage = update.getMessage().getText().toLowerCase();

        switch (tgUser.getCommandSate()) {
            case LOGS_WAIT_FOR_CHOOSING:
                return handleAndReturnSendObjectSecured(update, tgUser, "logsCommand", LogsCommand.class);

            case SEND_MESSAGE_WAIT_FOR_MESSAGE:
                return handleAndReturnSendObjectSecured(update, tgUser, "sendMessageCommand", SendMessageCommand.class);
        }

        if (AdminMenu.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "adminMenu", AdminMenu.class);
        }
        if (LogsCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "logsCommand", LogsCommand.class);
        }
        if (SendMessageCommand.getNames().contains(userMessage)) {
            return handleAndReturnSendObjectSecured(update, tgUser, "sendMessageCommand", SendMessageCommand.class);
        }

        return null;
    }

    private <T> Object handleAndReturnSendObject(Update update,
                                                 TelegramUser tgUser,
                                                 String beanName,
                                                 Class<? extends Command> clazz) {
        command = context.getBean(beanName, clazz);

        command.handle(update, tgUser);

        if (command.getSendObjects() != null && command.getSendObjects().length != 0) {
            return command.getSendObjects();
        }

        if (command.getSendMessage() != null && command.getSendMessage().getText() != null) {
            return command.getSendMessage();
        }

        return command.getSendMessage();
    }

    private <T> Object handleAndReturnSendObjectSecured(Update update,
                                                        TelegramUser tgUser,
                                                        String beanName,
                                                        Class<? extends Command> clazz) {
        command = context.getBean(beanName, clazz);

        command.handle(update, tgUser, new AccessTokenDTO(), SecurityState.OK);

        if (command.getSendObjects() != null && command.getSendObjects().length != 0) {
            return command.getSendObjects();
        }

        if (command.getSendMessage() != null && command.getSendMessage().getText() != null) {
            return command.getSendMessage();
        }

        return null;
    }
}
