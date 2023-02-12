package me.kqlqk.behealthy.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import me.kqlqk.behealthy.tgbot.service.command.CommandState;

import javax.persistence.*;

@Entity
@Table(name = "telegram_user", schema = "public", catalog = "tg_bot_db")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class TelegramUser {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private long id;

    @Column(name = "telegram_id", unique = true, nullable = false)
    private long telegramId;

    @Column(name = "user_id", unique = true)
    private long userId;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Enumerated(EnumType.STRING)
    @Column(name = "command_state", nullable = false)
    private CommandState commandSate;

    @Column(name = "active", nullable = false)
    private boolean active;
}
