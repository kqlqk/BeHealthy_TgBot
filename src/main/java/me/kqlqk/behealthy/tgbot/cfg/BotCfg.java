package me.kqlqk.behealthy.tgbot.cfg;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.service.UpdateService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class BotCfg extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    private static final List<BotCommand> botCommands = new ArrayList<>();

    private UpdateService updateService;


    @PostConstruct
    public void register() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);

            addCommands();
            this.execute(new SetMyCommands(botCommands, new BotCommandScopeDefault(), null));
        }
        catch (TelegramApiException e) {
            log.error("Something went wrong: ", e);
            throw new RuntimeException(e);
        }
    }


    private void addCommands() {
        botCommands.add(new BotCommand("/start", "Start the bot"));
        botCommands.add(new BotCommand("/login", "Sign in"));
        botCommands.add(new BotCommand("/registration", "Sign up"));
        botCommands.add(new BotCommand("/me", "Information about your account"));
        botCommands.add(new BotCommand("/get_condition", "Get your body condition"));
        botCommands.add(new BotCommand("/set_condition", "Add your body condition"));
        botCommands.add(new BotCommand("/set_condition_no_fat_percent", "Add your body condition if you don't know your body fat percent"));
        botCommands.add(new BotCommand("/update_condition", "Update your body condition"));
        botCommands.add(new BotCommand("/daily_kcals", "Get your daily kcals"));
        botCommands.add(new BotCommand("/add_food", "Add your food for tracking"));
        botCommands.add(new BotCommand("/get_food", "Get your ate food"));
        botCommands.add(new BotCommand("/delete_food", "Delete your ate food"));
        botCommands.add(new BotCommand("/create_workout", "Create your workout plan"));
        botCommands.add(new BotCommand("/get_workout", "Get your workout plan"));
        botCommands.add(new BotCommand("/update_workout", "Update your workout plan"));
        botCommands.add(new BotCommand("/update_workout_by_alternative", "Update your workout plan by alternative exercise"));
        botCommands.add(new BotCommand("/get_exercise_by_name", "Get exercise by name"));
        botCommands.add(new BotCommand("/get_exercises_by_muscle_group", "Get exercise by muscle group"));
        botCommands.add(new BotCommand("/change_kcals_goal", "Change kilocalories goal"));
    }


    @Override
    public void onUpdateReceived(Update update) {
        Object answer = updateService.handle(update);

        if (answer != null) {
            sendSth(answer);
        }
    }


    private void sendSth(Object answer) {
        try {
            if (answer instanceof SendMessage) {
                execute((SendMessage) answer);
            }
            else if (answer instanceof SendMessage[]) {
                for (SendMessage s : (SendMessage[]) answer) {
                    execute(s);
                }
            }
        }
        catch (TelegramApiException e) {
            log.error("Something went wrong: ", e);
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public void setUpdateService(UpdateService updateService) {
        this.updateService = updateService;
    }

    @Override
    public String getBotUsername() {
        return botName;
    }

    @Override
    public String getBotToken() {
        return botToken;
    }
}
