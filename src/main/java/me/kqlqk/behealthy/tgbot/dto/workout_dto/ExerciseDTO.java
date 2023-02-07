package me.kqlqk.behealthy.tgbot.dto.workout_dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ExerciseDTO {
    private int id;
    private String name;
    private String description;
    private String muscleGroup;
    private boolean hasAlternative;
}
