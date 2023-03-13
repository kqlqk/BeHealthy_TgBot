package me.kqlqk.behealthy.tgbot.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.persistence.*;

@Entity
@Table(name = "all_chats", schema = "public", catalog = "tg_bot_db")
@AllArgsConstructor
@NoArgsConstructor
@Data
public class ChatId {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", unique = true, nullable = false, insertable = false, updatable = false)
    private long id;

    @Column(name = "chat_id", unique = true, nullable = false)
    private String chatId;
}
