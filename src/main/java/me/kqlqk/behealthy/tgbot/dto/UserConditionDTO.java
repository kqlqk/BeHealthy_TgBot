package me.kqlqk.behealthy.tgbot.dto;

import lombok.Data;

@Data
public class UserConditionDTO {
    private long id;
    private String gender;
    private byte age;
    private short height;
    private short weight;
    private String intensity;
    private String goal;
    private double fatPercent;
}
