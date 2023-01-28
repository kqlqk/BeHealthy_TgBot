package me.kqlqk.behealthy.tgbot.cfg;

import lombok.NonNull;
import org.telegram.telegrambots.meta.api.objects.commands.BotCommand;

public class MyBotCommand extends BotCommand {
    private final String beanName;
    private final String command;

    public MyBotCommand(@NonNull String command, @NonNull String description, @NonNull String beanName) {
        super(command, description);
        this.command = command;
        this.beanName = beanName;
    }

    @NonNull
    @Override
    public String getCommand() {
        return command;
    }

    public String getBeanName() {
        return beanName;
    }
}
