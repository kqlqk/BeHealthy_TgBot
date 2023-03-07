package me.kqlqk.behealthy.tgbot.dto.workout_service;

import lombok.Data;

@Data
public class AddUserWorkoutDTO {
    private String exerciseName;
    private int rep;
    private int set;
    private int numberPerDay;
    private int day;
}
