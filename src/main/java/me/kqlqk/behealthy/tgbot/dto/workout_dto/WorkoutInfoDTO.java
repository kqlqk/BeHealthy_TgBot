package me.kqlqk.behealthy.tgbot.dto.workout_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WorkoutInfoDTO {
    private long id;
    private int day;
    private int numberPerDay;
    private ExerciseDTO exercise;
    private int reps;
    private int sets;
    private int workoutsPerWeek;
}
