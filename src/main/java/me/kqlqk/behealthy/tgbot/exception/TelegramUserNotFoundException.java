package me.kqlqk.behealthy.tgbot.exception;

public class TelegramUserNotFoundException extends RuntimeException {
    public TelegramUserNotFoundException(String message) {
        super(message);
    }
}
