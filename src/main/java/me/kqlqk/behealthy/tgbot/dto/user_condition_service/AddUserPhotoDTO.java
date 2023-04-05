package me.kqlqk.behealthy.tgbot.dto.user_condition_service;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AddUserPhotoDTO {
    private String photoDate;
    private String encodedPhoto;
}
