package me.kqlqk.behealthy.tgbot.dto.conditionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConditionWithoutFatPercentFemaleDTO {
    private int age;
    private int height;
    private int weight;
    private String intensity;
    private String goal;
    int fatFoldBetweenShoulderAndElbow;
    int fatFoldBetweenChestAndIlium;
    int fatFoldBetweenNavelAndLowerBelly;
}
