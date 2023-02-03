package me.kqlqk.behealthy.tgbot.dto.conditionService;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DailyKcalsDTO {
    private long id;
    private int protein;
    private int fat;
    private int carb;
}
