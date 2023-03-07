package me.kqlqk.behealthy.tgbot.dto.workout_service;

import lombok.Data;

@Data
public class GetWorkoutInfoDTO {
    private int day;
    private int numberPerDay;
    private GetExerciseDTO exercise;
    private int rep;
    private int set;
    private int workoutsPerWeek;
}
