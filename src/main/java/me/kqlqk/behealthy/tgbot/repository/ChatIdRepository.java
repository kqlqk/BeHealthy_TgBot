package me.kqlqk.behealthy.tgbot.repository;

import me.kqlqk.behealthy.tgbot.model.ChatId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatIdRepository extends JpaRepository<ChatId, Long> {
    boolean existsByChatId(String chatId);
}
