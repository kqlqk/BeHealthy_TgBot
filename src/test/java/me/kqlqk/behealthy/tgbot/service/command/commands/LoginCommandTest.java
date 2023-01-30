package me.kqlqk.behealthy.tgbot.service.command.commands;

import annotations.ServiceTest;
import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.UserDTO;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.enums.CommandState;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.when;

@ServiceTest
@ExtendWith(MockitoExtension.class)
public class LoginCommandTest {
    @Autowired
    private LoginCommand loginCommand;

    @MockBean
    private GatewayClient gatewayClient;

    @Autowired
    private TelegramUserService telegramUserService;

    @Mock
    private Update update;

    @Mock
    private Message message;

    @Test
    public void handle_shouldUpdateUser() {
        when(update.getMessage()).thenReturn(message);
        when(message.getChatId()).thenReturn(1L);

        TelegramUser tgUser = telegramUserService.getByTelegramId(1);

        loginCommand.handle(update, tgUser);

        TelegramUser updatedTgUser = telegramUserService.getByTelegramId(1);

        assertThat(updatedTgUser.isActive()).isFalse();
        assertThat(updatedTgUser.getCommandSate()).isEqualTo(CommandState.LOGIN_WAIT_FOR_USERNAME_AND_PASSWORD);
    }

    @Test
    public void handle_shouldLogInUser() {
        when(update.getMessage()).thenReturn(message);
        when(message.getText()).thenReturn("email pswd");
        when(message.getChatId()).thenReturn(1L);

        UserDTO userDTO = new UserDTO();
        userDTO.setEmail("email");
        userDTO.setPassword("pswd");
        when(gatewayClient.logInUser(userDTO)).thenReturn(new TokensDTO(2, "access", "refresh"));

        loginCommand.handle(update, telegramUserService.getByTelegramId(1));

        TelegramUser tgUser = telegramUserService.getByTelegramId(1);


        loginCommand.handle(update, tgUser);


        TelegramUser updatedTgUser = telegramUserService.getByTelegramId(1);

        assertThat(updatedTgUser.isActive()).isTrue();
        assertThat(updatedTgUser.getCommandSate()).isEqualTo(CommandState.BASIC);
    }

}
