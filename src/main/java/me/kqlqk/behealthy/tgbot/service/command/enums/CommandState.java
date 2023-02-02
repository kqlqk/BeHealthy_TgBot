package me.kqlqk.behealthy.tgbot.service.command.enums;

public enum CommandState {
    BASIC,

    LOGIN_WAIT_FOR_DATA,

    REGISTRATION_WAIT_FOR_DATA,

    SET_CONDITION_WAIT_FOR_DATA,

    SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_GENDER,
    SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_MALE,
    SET_CONDITION_NO_FAT_PERCENT_WAIT_FOR_DATA_FEMALE
}
