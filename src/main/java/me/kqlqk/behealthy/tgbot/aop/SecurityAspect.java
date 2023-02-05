package me.kqlqk.behealthy.tgbot.aop;

import lombok.extern.slf4j.Slf4j;
import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.exception.BadUserDataException;
import me.kqlqk.behealthy.tgbot.exception.NoRightsException;
import me.kqlqk.behealthy.tgbot.feign.GatewayClient;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.telegram.telegrambots.meta.api.objects.Update;

@Aspect
@Component
@Slf4j
public class SecurityAspect {
    private final TelegramUserService telegramUserService;
    private final GatewayClient gatewayClient;

    @Autowired
    public SecurityAspect(TelegramUserService telegramUserService, GatewayClient gatewayClient) {
        this.telegramUserService = telegramUserService;
        this.gatewayClient = gatewayClient;
    }


    @Before("@annotation(SecurityCheck)")
    private void beforeCheckUserIdAnnotation(JoinPoint joinPoint) {
        Update update = null;

        for (Object arg : joinPoint.getArgs()) {
            if (arg instanceof Update) {
                update = (Update) arg;
                break;
            }
            else {
                throw new IllegalArgumentException("Class instance of Update not found");
            }
        }

        TelegramUser tgUser = telegramUserService.getByTelegramId(update.getMessage().getFrom().getId());

        if (!tgUserAndFieldsNotNull(tgUser)) {
            log.warn("User with telegramId = " + tgUser.getTelegramId() + " tried to get access (beforeCheckUserIdAnnotation)");
            throw new NoRightsException("You have no rights to do this");
        }
    }

    @Around("@annotation(SecurityCheck)")
    private void setAccessTokenToMethod(ProceedingJoinPoint proceedingJoinPoint) throws Throwable {
        TelegramUser tgUser = null;

        for (Object arg : proceedingJoinPoint.getArgs()) {
            if (arg instanceof TelegramUser) {
                tgUser = (TelegramUser) arg;
                break;
            }
        }

        if (tgUser == null) {
            log.warn("Telegram user is null");
            throw new BadUserDataException("Telegram user is null");
        }

        Object[] modifiedArgs = proceedingJoinPoint.getArgs();

        int indexForTokens = 0;
        int indexForSecurityState = 0;
        boolean hasTokensDTO = false;
        boolean hasSecurityState = false;

        for (Object arg : proceedingJoinPoint.getArgs()) {
            if (arg instanceof TokensDTO) {
                hasTokensDTO = true;
                break;
            }
            indexForTokens++;
        }

        for (Object arg : proceedingJoinPoint.getArgs()) {
            if (arg instanceof SecurityState) {
                hasSecurityState = true;
                break;
            }
            indexForSecurityState++;
        }

        if (hasTokensDTO) {
            TokensDTO refreshTokenDTO = new TokensDTO(0, null, tgUser.getRefreshToken());

            TokensDTO accessTokenDTO;
            try {
                accessTokenDTO = gatewayClient.getNewAccessToken(refreshTokenDTO);
                accessTokenDTO.setAccessToken("Bearer " + accessTokenDTO.getAccessToken());

                modifiedArgs[indexForTokens] = accessTokenDTO;
            }
            catch (RuntimeException e) {
                if (hasSecurityState) {
                    modifiedArgs[indexForSecurityState] = SecurityState.SHOULD_RELOGIN;
                }
                else {
                    log.warn("Method hasn't security state", e);
                    throw e;
                }
            }
        }

        proceedingJoinPoint.proceed(modifiedArgs);

        log.info("User with userId = " + tgUser.getUserId() + " got access (setAccessTokenToMethod)");
    }

    private boolean tgUserAndFieldsNotNull(TelegramUser tgUser) {
        return tgUser != null &&
                tgUser.getId() != 0 &&
                tgUser.getUserId() != 0 &&
                tgUser.getRefreshToken() != null &&
                tgUser.getCommandSate() != null &&
                tgUser.isActive();
    }

}
