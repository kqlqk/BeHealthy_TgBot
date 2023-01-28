package me.kqlqk.behealthy.tgbot.cfg;

import me.kqlqk.behealthy.tgbot.service.Command;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
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
public class BotCfg extends TelegramLongPollingBot {
    @Value("${bot.name}")
    private String botName;

    @Value("${bot.token}")
    private String botToken;

    private static final List<MyBotCommand> myBotCommands = new ArrayList<>();
    private ApplicationContext context;


    @PostConstruct
    public void register() {
        try {
            TelegramBotsApi api = new TelegramBotsApi(DefaultBotSession.class);
            api.registerBot(this);

            addCommands();
            this.execute(new SetMyCommands(new ArrayList<>(myBotCommands), new BotCommandScopeDefault(), null));
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }


    private void addCommands() {
        myBotCommands.add(new MyBotCommand("/start", "Start the bot", "startCommand"));
    }


    @Override
    public void onUpdateReceived(Update update) {
        if (isCommand(update)) {
            MyBotCommand myBotCommand = getMyBotCommandByCommand(update.getMessage().getText());
            Command command = context.getBean(myBotCommand.getBeanName(), Command.class);

            command.handle(update);

            if (command.getSendMessage() != null) {
                sendMessage(command.getSendMessage());
            }
        }

    }


    private boolean isCommand(Update update) {
        if (!update.hasMessage() && !update.getMessage().hasText()) {
            return false;
        }

        for (BotCommand command : myBotCommands) {
            if (command.getCommand().equalsIgnoreCase(update.getMessage().getText())) {
                return true;
            }
        }

        return false;
    }

    public MyBotCommand getMyBotCommandByCommand(String command) {
        for (MyBotCommand myBotCommand : myBotCommands) {
            if (myBotCommand.getCommand().equalsIgnoreCase(command)) {
                return myBotCommand;
            }
        }

        return null;
    }

    private void sendMessage(SendMessage message) {
        try {
            execute(message);
        } catch (TelegramApiException e) {
            throw new RuntimeException(e);
        }
    }

    @Autowired
    public void setContext(ApplicationContext context) {
        this.context = context;
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
