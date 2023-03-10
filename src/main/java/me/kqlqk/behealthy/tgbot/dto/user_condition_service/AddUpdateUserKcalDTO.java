package me.kqlqk.behealthy.tgbot.dto.user_condition_service;

import lombok.Data;

@Data
public class AddUpdateUserKcalDTO {
    private int kcal;
    private int protein;
    private int fat;
    private int carb;
    private boolean inPriority;
    private boolean onlyKcal;
}
