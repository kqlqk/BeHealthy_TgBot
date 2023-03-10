package me.kqlqk.behealthy.tgbot.dto.user_condition_service;

import lombok.Data;

@Data
public class GetDailyKcalDTO {
    private int protein;
    private int fat;
    private int carb;
}
