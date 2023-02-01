package me.kqlqk.behealthy.tgbot.service.command.enums;

public enum CommandState {
    BASIC,

    LOGIN_WAIT_FOR_USERNAME_AND_PASSWORD,

    REGISTRATION_WAIT_FOR_EMAIL,
    REGISTRATION_WAIT_FOR_NAME,
    REGISTRATION_WAIT_FOR_PASSWORD,

    SET_CONDITION_WAIT_FOR_DATA
}
