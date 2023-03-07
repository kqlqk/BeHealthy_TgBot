package me.kqlqk.behealthy.tgbot.dto.condition_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddDailyAteFoodDTO {
    private String name;
    private double weight;
    private int protein;
    private int fat;
    private int carb;
}
