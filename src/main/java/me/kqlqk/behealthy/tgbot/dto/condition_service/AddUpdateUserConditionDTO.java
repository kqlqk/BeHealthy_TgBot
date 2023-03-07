package me.kqlqk.behealthy.tgbot.dto.condition_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUpdateUserConditionDTO {
    private String gender;
    private int age;
    private int height;
    private int weight;
    private String intensity;
    private String goal;
    private double fatPercent;
    private boolean fatPercentExists;
    private int fatFoldBetweenChestAndIlium;
    private int fatFoldBetweenNavelAndLowerBelly;
    private int fatFoldBetweenNippleAndArmpit;
    private int fatFoldBetweenNippleAndUpperChest;
    private int fatFoldBetweenShoulderAndElbow;

    public AddUpdateUserConditionDTO(String gender,
                                     int age,
                                     int height,
                                     int weight,
                                     String intensity,
                                     String goal,
                                     double fatPercent,
                                     boolean fatPercentExists) {
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.intensity = intensity;
        this.goal = goal;
        this.fatPercent = fatPercent;
        this.fatPercentExists = fatPercentExists;
    }
}
