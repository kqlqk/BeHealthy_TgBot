package me.kqlqk.behealthy.tgbot.dto.conditionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserConditionDTO {
    private long id;
    private String gender;
    private int age;
    private int height;
    private int weight;
    private String intensity;
    private String goal;
    private double fatPercent;

    public UserConditionDTO(String gender, int age, int height, int weight, String intensity, String goal, double fatPercent) {
        this.gender = gender;
        this.age = age;
        this.height = height;
        this.weight = weight;
        this.intensity = intensity;
        this.goal = goal;
        this.fatPercent = fatPercent;
    }
}
