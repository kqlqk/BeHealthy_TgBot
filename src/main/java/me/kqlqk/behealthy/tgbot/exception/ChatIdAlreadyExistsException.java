package me.kqlqk.behealthy.tgbot.exception;

public class ChatIdAlreadyExistsException extends RuntimeException {
    public ChatIdAlreadyExistsException(String message) {
        super(message);
    }
}
