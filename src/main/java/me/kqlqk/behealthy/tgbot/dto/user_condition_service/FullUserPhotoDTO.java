package me.kqlqk.behealthy.tgbot.dto.user_condition_service;

import lombok.Data;

import java.util.Date;

@Data
public class FullUserPhotoDTO {
    private String photoPath;
    private Date photoDate;
    private String encodedPhoto;
}
