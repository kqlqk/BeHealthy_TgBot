package me.kqlqk.behealthy.tgbot.dto.workout_service;

import lombok.Data;

@Data
public class GetExerciseDTO {
    private String name;
    private String description;
    private String muscleGroup;
    private Integer alternativeId;
}
