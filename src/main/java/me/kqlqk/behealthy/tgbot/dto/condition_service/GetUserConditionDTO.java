package me.kqlqk.behealthy.tgbot.dto.condition_service;

import lombok.Data;

@Data
public class GetUserConditionDTO {
    private GetDailyKcalDTO dailyKcalDTO;
    private String gender;
    private int age;
    private int height;
    private int weight;
    private String intensity;
    private String goal;
    private double fatPercent;
}
