package me.kqlqk.behealthy.tgbot.dto.user_condition_service;

import lombok.Data;

@Data
public class GetDailyAteFoodDTO {
    private long id;
    private String name;
    private double weight;
    private int kcal;
    private int protein;
    private int fat;
    private int carb;
    private boolean today;
}
