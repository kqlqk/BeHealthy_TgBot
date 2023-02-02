package me.kqlqk.behealthy.tgbot.service.impl;

import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.authService.TokensDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.UpdateService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.commands.guest.DefaultCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.guest.LoginCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.guest.RegistrationCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.guest.StartCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.user.GetConditionCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.user.MeCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.user.SetConditionCommand;
import me.kqlqk.behealthy.tgbot.service.command.commands.user.SetConditionNoFatPercentCommand;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.objects.Update;

@Service
public class UpdateServiceImpl implements UpdateService {
    private final TelegramUserService telegramUserService;
    private final ApplicationContext context;
    private Command command;

    @Autowired
    public UpdateServiceImpl(TelegramUserService telegramUserService, ApplicationContext context) {
        this.telegramUserService = telegramUserService;
        this.context = context;
    }

    @Override
    public Object handle(Update update) {
        if (!update.hasMessage()) {
            return null;
        }

        long tgId = update.getMessage().getFrom().getId();

        if (!telegramUserService.existsByTelegramId(tgId)) {
            TelegramUser newTgUser = new TelegramUser();
            newTgUser.setTelegramId(tgId);
            newTgUser.setActive(false);
            newTgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.save(newTgUser);
        }

        TelegramUser tgUser = telegramUserService.getByTelegramId(tgId);

        if (!tgUser.isActive()) {
            return choosingForInactiveUsers(update, tgUser);
        }

        if (tgUser.getCommandSate() != CommandState.BASIC) {
            return choosingBetweenCommandState(update, tgUser);
        }

        return choosingBetweenCommands(update, tgUser);
    }

    private Object choosingForInactiveUsers(Update update, TelegramUser tgUser) {
        switch (tgUser.getCommandSate()) {
            case LOGIN_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case REGISTRATION_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);
        }

        switch (update.getMessage().getText()) {
            case "/start":
                return handleAndReturnSendObject(update, tgUser, "startCommand", StartCommand.class);

            case "/login":
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case "/registration":
                return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);

            default:
                return handleAndReturnSendObject(update, tgUser, "defaultCommand", DefaultCommand.class);
        }
    }

    private Object choosingBetweenCommands(Update update, TelegramUser tgUser) {
        switch (update.getMessage().getText()) {
            case "/start":
                return handleAndReturnSendObject(update, tgUser, "startCommand", StartCommand.class);

            case "/login":
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case "/registration":
                return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);

            case "/me":
                return handleAndReturnSendObject(update, tgUser, "meCommand", MeCommand.class, new TokensDTO(), SecurityState.OK);

            case "/get_condition":
                return handleAndReturnSendObject(update, tgUser, "getConditionCommand", GetConditionCommand.class,
                                                 new TokensDTO(), SecurityState.OK);

            case "/set_condition":
                return handleAndReturnSendObject(update, tgUser, "setConditionCommand", SetConditionCommand.class,
                                                 new TokensDTO(), SecurityState.OK);

            case "/set_condition_no_fat_percent":
                return handleAndReturnSendObject(update, tgUser, "setConditionNoFatPercentCommand", SetConditionNoFatPercentCommand.class,
                                                 new TokensDTO(), SecurityState.OK);

            default:
                return null;
        }
    }

    private Object choosingBetweenCommandState(Update update, TelegramUser tgUser) {
        switch (tgUser.getCommandSate()) {
            case LOGIN_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "loginCommand", LoginCommand.class);

            case REGISTRATION_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "registrationCommand", RegistrationCommand.class);

            case SET_CONDITION_WAIT_FOR_DATA:
                return handleAndReturnSendObject(update, tgUser, "setConditionCommand", SetConditionCommand.class,
                                                 new TokensDTO(), SecurityState.OK);

            case SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_GENDER:
            case SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_MALE:
            case SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_FEMALE:
                return handleAndReturnSendObject(update, tgUser, "setConditionNoFatPercentCommand", SetConditionNoFatPercentCommand.class,
                                                 new TokensDTO(), SecurityState.OK);
        }

        return null;
    }

    private <T> Object handleAndReturnSendObject(Update update,
                                                 TelegramUser tgUser,
                                                 String beanName,
                                                 Class<? extends Command> clazz) {
        command = context.getBean(beanName, clazz);

        command.handle(update, tgUser);

        return command.getSendMessage();
    }

    private <T> Object handleAndReturnSendObject(Update update,
                                                 TelegramUser tgUser,
                                                 String beanName,
                                                 Class<? extends Command> clazz,
                                                 TokensDTO tokensDTO,
                                                 SecurityState securityState) {
        command = context.getBean(beanName, clazz);

        command.handle(update, tgUser, tokensDTO, securityState);

        return command.getSendMessage();
    }
}
