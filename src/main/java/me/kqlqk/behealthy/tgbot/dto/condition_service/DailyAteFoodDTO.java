package me.kqlqk.behealthy.tgbot.dto.condition_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyAteFoodDTO {
    private long id;
    private String name;
    private double weight;
    private double kcals;
    private double proteins;
    private double fats;
    private double carbs;

    public DailyAteFoodDTO(String name, double weight, double proteins, double fats, double carbs) {
        this.name = name;
        this.weight = weight;
        this.proteins = proteins;
        this.fats = fats;
        this.carbs = carbs;
    }
}
