package me.kqlqk.behealthy.tgbot.util;


import java.util.LinkedHashMap;
import java.util.Map;

public class Maps {
    private static final Map<Long, Boolean> userIdOnlyKcal = new LinkedHashMap<>();
    private static final Map<Long, Boolean> userIdFatPercent = new LinkedHashMap<>();
    private static final Map<Long, String> userIdGender = new LinkedHashMap<>();
    private static final Map<Long, String> userIdActivity = new LinkedHashMap<>();
    private static final Map<Long, String> userIdGoal = new LinkedHashMap<>();

    public static void removeFromAll(long userId) {
        removeUserIdFatPercent(userId);
        removeUserIdGender(userId);
        removeUserIdGoal(userId);
        removeUserIdOnlyKcal(userId);
        removeUserIdActivity(userId);
    }

    public static void putUserIdOnlyKcal(long userId, boolean onlyKcal) {
        if (userIdOnlyKcal.size() > 50) {
            userIdOnlyKcal.remove(userIdOnlyKcal.entrySet().iterator().next());
        }

        userIdOnlyKcal.put(userId, onlyKcal);
    }

    public static void removeUserIdOnlyKcal(long userId) {
        userIdOnlyKcal.remove(userId);
    }

    public static boolean getUserIdOnlyKcal(long userId) {
        return userIdOnlyKcal.get(userId);
    }


    public static void putUserIdFatPercent(long userId, boolean fatPercent) {
        if (userIdFatPercent.size() > 50) {
            userIdFatPercent.remove(userIdFatPercent.entrySet().iterator().next());
        }

        userIdFatPercent.put(userId, fatPercent);
    }

    public static void removeUserIdFatPercent(long userId) {
        userIdFatPercent.remove(userId);
    }

    public static boolean getUserIdFatPercent(long userId) {
        return userIdFatPercent.get(userId);
    }


    public static void putUserIdGender(long userId, String gender) {
        if (userIdGender.size() > 50) {
            userIdGender.remove(userIdGender.entrySet().iterator().next());
        }

        userIdGender.put(userId, gender);
    }

    public static void removeUserIdGender(long userId) {
        userIdGender.remove(userId);
    }

    public static String getUserIdGender(long userId) {
        return userIdGender.get(userId);
    }


    public static void putUserIdActivity(long userId, String activity) {
        if (userIdActivity.size() > 50) {
            userIdActivity.remove(userIdActivity.entrySet().iterator().next());
        }

        userIdActivity.put(userId, activity);
    }

    public static void removeUserIdActivity(long userId) {
        userIdActivity.remove(userId);
    }

    public static String getUserIdActivity(long userId) {
        return userIdActivity.get(userId);
    }


    public static void putUserIdGoal(long userId, String goal) {
        if (userIdGoal.size() > 50) {
            userIdGoal.remove(userIdGoal.entrySet().iterator().next());
        }

        userIdGoal.put(userId, goal);
    }

    public static void removeUserIdGoal(long userId) {
        userIdGoal.remove(userId);
    }

    public static String getUserIdGoal(long userId) {
        return userIdGoal.get(userId);
    }
}
