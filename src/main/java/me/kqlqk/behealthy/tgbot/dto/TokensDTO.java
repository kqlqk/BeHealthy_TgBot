package me.kqlqk.behealthy.tgbot.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TokensDTO {
    private long userId;
    private String accessToken;
    private String refreshToken;
}