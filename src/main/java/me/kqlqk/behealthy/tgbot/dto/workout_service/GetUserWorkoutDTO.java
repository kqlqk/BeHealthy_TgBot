package me.kqlqk.behealthy.tgbot.dto.workout_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GetUserWorkoutDTO {
    private String exerciseName;
    private int rep;
    private int set;
    private int numberPerDay;
    private int day;
}
