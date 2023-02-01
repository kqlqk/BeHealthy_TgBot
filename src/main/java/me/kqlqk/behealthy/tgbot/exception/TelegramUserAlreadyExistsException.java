package me.kqlqk.behealthy.tgbot.exception;

public class TelegramUserAlreadyExistsException extends RuntimeException {
    public TelegramUserAlreadyExistsException(String message) {
        super(message);
    }
}
