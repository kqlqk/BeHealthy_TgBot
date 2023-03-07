package me.kqlqk.behealthy.tgbot.dto.workout_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUpdateWorkoutInfoDTO {
    private int workoutsPerWeek;
}
