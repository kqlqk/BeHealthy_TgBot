package me.kqlqk.behealthy.tgbot.cfg;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.service.UpdateService;
import me.kqlqk.behealthy.tgbot.util.TmpFiles;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.bots.TelegramLongPollingBot;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.GetFile;
import org.telegram.telegrambots.meta.api.methods.commands.SetMyCommands;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.methods.send.SendPhoto;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.DeleteMessage;
import org.telegram.telegrambots.meta.api.methods.updatingmessages.EditMessageText;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;
import org.telegram.telegrambots.meta.api.objects.commands.scope.BotCommandScopeDefault;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.updatesreceivers.DefaultBotSession;

import javax.annotation.PostConstruct;
import java.io.IOException;
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
    private TmpFiles tmpFiles;


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
            if (answer instanceof Object[]) {
                for (Object a : (Object[]) answer) {
                    executeSth(a);
                }
            }
            executeSth(answer);
        }
        catch (TelegramApiException e) {
            log.error("Something went wrong: ", e);
            throw new RuntimeException(e);
        }
    }

    public Object executeSth(Object obj) throws TelegramApiException {
        if (obj instanceof SendMessage) {
            return execute((SendMessage) obj);
        }
        else if (obj instanceof EditMessageText) {
            return execute((EditMessageText) obj);
        }
        else if (obj instanceof DeleteMessage) {
            return execute((DeleteMessage) obj);
        }
        else if (obj instanceof SendPhoto) {
            Object o = execute((SendPhoto) obj);
            try {
                tmpFiles.cleanByName(((SendPhoto) obj).getPhoto().getMediaName());
            }
            catch (IOException e) {
                throw new RuntimeException(e);
            }

            return o;
        }
        else if (obj instanceof GetFile) {
            return execute((GetFile) obj);
        }
        else {
            return null;
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

    @Autowired
    public void setTmpFiles(TmpFiles tmpFiles) {
        this.tmpFiles = tmpFiles;
    }
}
