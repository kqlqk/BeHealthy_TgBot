package me.kqlqk.behealthy.tgbot.dto.conditionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConditionWithoutFatPercentMaleDTO {
    private int age;
    private int height;
    private int weight;
    private String intensity;
    private String goal;
    int fatFoldBetweenChestAndIlium;
    int fatFoldBetweenNavelAndLowerBelly;
    int fatFoldBetweenNippleAndArmpit;
    int fatFoldBetweenNippleAndUpperChest;
}
