package me.kqlqk.behealthy.tgbot.feign;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class CustomErrorDecoder implements ErrorDecoder {

    @Override
    public Exception decode(String s, Response response) {
        Map<String, String> info;

        try (InputStream body = response.body().asInputStream()) {
            ObjectMapper objectMapper = new ObjectMapper();
            info = objectMapper.readValue(body, Map.class);
        }
        catch (IOException e) {
            if (e instanceof JsonParseException) {
                throw new RuntimeException("Service is unavailable");
            }

            throw new RuntimeException(e.getMessage());
        }

        String errorMessage;
        if (info.get("info") != null) {
            errorMessage = info.get("info");
        }
        else {
            log.error("Something went wrong: " + info.keySet().stream()
                    .map(key -> key + "=" + info.get(key))
                    .collect(Collectors.joining(", ", "{", "}")));

            return new RuntimeException("Something went wrong");
        }


        if (errorMessage.matches("Bad credentials") ||
                errorMessage.matches("Name should contains only letters") ||
                errorMessage.matches("Name should be between 2 and 20 characters") ||
                errorMessage.matches("Email should be valid") ||
                errorMessage.matches("Password should be between 8 and 50 characters, no spaces, at least: 1 number, 1 uppercase letter, 1 lowercase letter") ||
                errorMessage.matches("Age should be between 15 and 60") ||
                errorMessage.matches("Height should be between 150 and 200") ||
                errorMessage.matches("Weight should be between 40 and 140") ||
                errorMessage.matches("Fat fold between chest and ilium should be between 2 and 50") ||
                errorMessage.matches("Fat fold between navel and lower belly should be between 5 and 70") ||
                errorMessage.matches("Fat fold between nipple and armpit should be between 2 and 50") ||
                errorMessage.matches("Fat fold between nipple and upper chest should be between 2 and 50") ||
                errorMessage.matches("Fat fold between shoulder and elbow should be between 2 and 50") ||
                errorMessage.matches("Name should be between 2 and 50 characters") ||
                errorMessage.matches("Day should be between 1 and 7")) {
            // ignore
        }
        else if (errorMessage.matches("Email cannot be null")) {
            errorMessage = "'Email' cannot be empty";
        }
        else if (errorMessage.matches("Password cannot be null")) {
            errorMessage = "'Password' cannot be empty";
        }
        else if (errorMessage.matches("User with email = [^ ]{1,50} not found")) {
            errorMessage = "User with that email not found, check email or sign up";
        }
        else if (errorMessage.matches("Name cannot be null")) {
            errorMessage = "'Name' cannot be empty";
        }
        else if (errorMessage.matches("User with email = [^ ]{1,50} already exists")) {
            errorMessage = "User with that email already exists, change email or sign in";
        }
        else if (errorMessage.matches("User with id = [^ ]{1,50} not found")) {
            errorMessage = "User not found";
        }
        else if (errorMessage.matches("Token for user with email = [^ ]{1,50} not found")) {
            errorMessage = "Something went wrong. Probably you are not signed up. Try to sign in / sign up";
        }
        else if (errorMessage.matches("User condition with userId = [^ ]{1,50} not found")) {
            errorMessage = "Condition not found. Check, if you have your body's condition";
        }
        else if (errorMessage.matches("Gender cannot be null")) {
            errorMessage = "'Gender' cannot be empty";
        }
        else if (errorMessage.matches("Activity cannot be null")) {
            errorMessage = "'Activity' cannot be empty";
        }
        else if (errorMessage.matches("Goal cannot be null")) {
            errorMessage = "'Goal' cannot be empty";
        }
        else if (errorMessage.matches("FatPercent should be between 3 and 40")) {
            errorMessage = "Percent of fat should be between 3 and 40";
        }
        else if (errorMessage.matches("User condition with userId = [^ ]{1,50} already exists")) {
            errorMessage = "You already set a body condition";
        }
        else if (errorMessage.matches("DailyAteFood with userId = [^ ]{1,50} not found")) {
            errorMessage = "Daily ate food not found. Probably you didn't add it";
        }
        else if (errorMessage.matches("DailyAteFood with name = [^ ]{1,50} and userId = [^ ]{1,50} not found")) {
            errorMessage = "Daily ate food not found. Probably you didn't add it";
        }
        else if (errorMessage.matches("DailyAteFood with name = [^ ]{1,50} and userId = [^ ]{1,50} already exists")) {
            errorMessage = "Daily ate food already exists.";
        }
        else if (errorMessage.matches("Weight should be > 0")) {
            errorMessage = "Weight should be greater than 0";
        }
        else if (errorMessage.matches("Weight should be < 10000")) {
            errorMessage = "Weight should be less than 10000";
        }
        else if (errorMessage.matches("Protein should be > -1")) {
            errorMessage = "Protein should be greater than -1";
        }
        else if (errorMessage.matches("Protein should be < 100")) {
            errorMessage = "Protein should be less than 100";
        }
        else if (errorMessage.matches("Fat should be > -1")) {
            errorMessage = "Fat should be greater than -1";
        }
        else if (errorMessage.matches("Fat should be < 100")) {
            errorMessage = "Fat should be less than 100";
        }
        else if (errorMessage.matches("Carb should be > -1")) {
            errorMessage = "Carb should be greater than -1";
        }
        else if (errorMessage.matches("Carb should be < 100")) {
            errorMessage = "Carb should be less than 100";
        }
        else if (errorMessage.matches("WorkoutInfos with userId = [^ ]{1,50} not found")) {
            errorMessage = "Workout not found";
        }
        else if (errorMessage.matches("WorkoutsPerWeek should be between 1 and 5")) {
            errorMessage = "You can have from 1 to 5 workouts per week";
        }
        else if (errorMessage.matches("Exercise with name = [^ ]{1,50} not found")) {
            errorMessage = "We haven't exercise with that name";
        }
        else if (errorMessage.matches("There are no alternative exercises for [^ ]{1,50}")) {
            errorMessage = "We haven't alternative exercise for provided exercise";
        }
        else if (errorMessage.matches("User's workout hasn't exercise with name = [^ ]{1,50}")) {
            errorMessage = "Your workout plan hasn't that exercise";
        }
        else if (errorMessage.matches("UserKcal with userId = [^ ]{1,50} not found")) {
            errorMessage = "You didn't set kilocalories goal";
        }
        else if (errorMessage.matches("UserKcal with userId = [^ ]{1,50} already exists")) {
            errorMessage = "Kilocalories goal already exists";
        }
        else if (errorMessage.matches("Please use valid gender \\(MALE or FEMALE\\)")) {
            errorMessage = "Invalid gender";
        }
        else if (errorMessage.matches("Please use valid activity \\(MIN or AVG or MAX\\)")) {
            errorMessage = "Invalid activity";
        }
        else if (errorMessage.matches("Please use valid goal \\(LOSE or MAINTAIN or GAIN\\)")) {
            errorMessage = "Invalid goal";
        }
        else if (errorMessage.matches("UserWorkout with userId = [^ ]{1,50} not found")) {
            errorMessage = "Your own workout not found";
        }
        else if (errorMessage.matches("ExerciseName should be between 1 and 50 characters")) {
            errorMessage = "Name of exercise should be between 1 and 50 characters";
        }
        else if (errorMessage.matches("MuscleGroup cannot be null")) {
            errorMessage = "MuscleGroup cannot be empty";
        }
        else if (errorMessage.matches("Rep should be between 0 and 1000")) {
            errorMessage = "Reps should be between 0 and 1000";
        }
        else if (errorMessage.matches("Set should be between 1 and 100")) {
            errorMessage = "Sets should be between 1 and 100";
        }
        else if (errorMessage.matches("NumberPerDay should be between 1 and 100")) {
            errorMessage = "Number per day should be between 1 and 100";
        }
        else if (errorMessage.matches("Exercise with exerciseName = [^ ]{1,50} for user with userId = [^ ]{1,50} not found")) {
            errorMessage = "Exercise with that name not found";
        }
        else if (errorMessage.matches("UserPhoto with userId = [^ ]{1,50} and date = [^ ]{1,50} not found")) {
            errorMessage = "Photo not found";
        }
        else if (errorMessage.matches("UserPhotos with userId = [^ ]{1,50} not found")) {
            errorMessage = "Photos not found";
        }
        else if (errorMessage.matches("UserPhoto with userId = [^ ]{1,50} and photoDate = [^ ]{1,50} already exists")) {
            errorMessage = "Photo already exists";
        }
        else {
            log.error("Unhandled exception: " + errorMessage);
            errorMessage = "Something went wrong";
        }

        return new RuntimeException(errorMessage);
    }
}

