package me.kqlqk.behealthy.tgbot.service.command;

import me.kqlqk.behealthy.tgbot.aop.SecurityState;
import me.kqlqk.behealthy.tgbot.dto.auth_service.AccessTokenDTO;
import me.kqlqk.behealthy.tgbot.model.TelegramUser;
import me.kqlqk.behealthy.tgbot.service.TelegramUserService;
import me.kqlqk.behealthy.tgbot.service.command.admin_menu.AdminMenu;
import me.kqlqk.behealthy.tgbot.service.command.body_condition_menu.BodyConditionMenu;
import me.kqlqk.behealthy.tgbot.service.command.kcals_tracker.KcalsTrackerMenu;
import me.kqlqk.behealthy.tgbot.service.command.workout_service.WorkoutServiceMenu;
import me.kqlqk.behealthy.tgbot.service.command.workout_service.commands.GetUserWorkoutCommand;
import me.kqlqk.behealthy.tgbot.util.Maps;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Service;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Update;

import java.util.ArrayList;
import java.util.List;

@Service
@Scope("prototype")
public class BackCommand extends Command {
    private SendMessage sendMessage;
    private final TelegramUserService telegramUserService;
    private final KcalsTrackerMenu kcalsTrackerMenu;
    private final GetUserWorkoutCommand getUserWorkoutCommand;

    public BackCommand(TelegramUserService telegramUserService, KcalsTrackerMenu kcalsTrackerMenu, GetUserWorkoutCommand getUserWorkoutCommand) {
        this.telegramUserService = telegramUserService;
        this.kcalsTrackerMenu = kcalsTrackerMenu;
        this.getUserWorkoutCommand = getUserWorkoutCommand;
    }

    @Override
    public void handle(Update update, TelegramUser tgUser) {
        String chatId = update.getMessage().getChatId().toString();

        sendMessage = new SendMessage(chatId, "Choose one of the following menu item");

        Maps.removeFromAll(tgUser.getUserId());

        switch (tgUser.getCommandSate()) {
            case ADD_FOOD_WAIT_FOR_DATA:
            case CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING:
            case CHANGE_KCAL_GOAL_WAIT_FOR_CHOOSING_KCAL:
            case CHANGE_KCAL_WAIT_FOR_DATA:
                kcalsTrackerMenu.handle(update, tgUser, new AccessTokenDTO(), SecurityState.OK);
                sendMessage = kcalsTrackerMenu.getSendMessage();
                sendMessage.setReplyMarkup(KcalsTrackerMenu.initKeyboard());
                break;

            case RETURN_TO_BODY_CONDITION_MENU:
            case SET_BODY_CONDITION_WAIT_FOR_FAT_PERCENT:
            case SET_BODY_CONDITION_WAIT_FOR_GENDER:
            case SET_BODY_CONDITION_WAIT_FOR_ACTIVITY:
            case SET_BODY_CONDITION_WAIT_FOR_GOAL:
            case SET_BODY_CONDITION_WAIT_FOR_DATA:
                sendMessage.setReplyMarkup(BodyConditionMenu.initKeyboard());
                break;

            case RETURN_TO_WORKOUT_SERVICE_MENU:
            case SET_WORKOUT_WAIT_FOR_DATA:
                sendMessage.setReplyMarkup(WorkoutServiceMenu.initKeyboard());
                break;

            case ADD_EXERCISE_WAIT_FOR_DATA:
            case REMOVE_EXERCISE_WAIT_FOR_DATA:
                getUserWorkoutCommand.handle(update, tgUser, new AccessTokenDTO(), SecurityState.OK);
                sendMessage = getUserWorkoutCommand.getSendMessage();
                sendMessage.setReplyMarkup(GetUserWorkoutCommand.initKeyboard());
                break;

            case LOGS_WAIT_FOR_CHOOSING:
            case SEND_MESSAGE_WAIT_FOR_MESSAGE:
                sendMessage.setReplyMarkup(AdminMenu.initKeyboard());
                break;

            default:
                sendMessage.setReplyMarkup(defaultKeyboard(tgUser.isActive()));
                break;
        }

        tgUser.setCommandSate(CommandState.BASIC);
        telegramUserService.update(tgUser);
    }

    public static List<String> getNames() {
        List<String> res = new ArrayList<>();
        res.add("/back");
        res.add("back ↩");
        res.add("back");

        return res;
    }

    @Override
    public SendMessage getSendMessage() {
        return sendMessage;
    }
}
