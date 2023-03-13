package me.kqlqk.behealthy.tgbot.service.command.admin_menu.commands;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.aop.SecurityCheck;
import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.model.ChatId;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.ChatIdService;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.Command;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;
import me.kqlqk.behealthy.tgbot.service.command.admin_menu.AdminMenu;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
@Slf4j
public class SendMessageCommand extends Command {
    private SendMessage sendMessage;
    private final List<SendMessage> sendMessages;

    private final TelegramUserService telegramUserService;
    private final ChatIdService chatIdService;

    @Autowired
    public SendMessageCommand(TelegramUserService telegramUserService, ChatIdService chatIdService) {
        this.telegramUserService = telegramUserService;
        this.chatIdService = chatIdService;
        sendMessages = new ArrayList<>();
    }

    @SecurityCheck
    @Override
    public void handle(Update update, TelegramUser tgUser, AccessTokenDTO accessTokenDTO, SecurityState securityState) {
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
            sendMessage = new SendMessage(chatId, "Send a message to all users");
            sendMessage.setReplyMarkup(onlyBackCommandKeyboard());

            tgUser.setCommandSate(CommandState.SEND_MESSAGE_WAIT_FOR_MESSAGE);
            telegramUserService.update(tgUser);
        }
        else if (tgUser.getCommandSate() == CommandState.SEND_MESSAGE_WAIT_FOR_MESSAGE) {
            for (ChatId chatIdEntity : chatIdService.getAll()) {
                SendMessage sendMessage = new SendMessage(chatIdEntity.getChatId(), userMessage);

                if (sendMessage.getChatId().equals(chatId)) {
                    sendMessage.setReplyMarkup(AdminMenu.initKeyboard());
                }

                sendMessages.add(sendMessage);
            }

            tgUser.setCommandSate(CommandState.BASIC);
            telegramUserService.update(tgUser);
        }
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("send message");
        res.add("/message");

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
