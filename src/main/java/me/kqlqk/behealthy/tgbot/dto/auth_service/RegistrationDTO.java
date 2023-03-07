package me.kqlqk.behealthy.tgbot.dto.auth_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RegistrationDTO {
    private String name;
    private String email;
    private String password;
}
