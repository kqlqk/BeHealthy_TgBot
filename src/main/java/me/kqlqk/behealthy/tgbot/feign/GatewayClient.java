package me.kqlqk.behealthy.tgbot.feign;

import me.kqlqk.behealthy.tgbot.dto.auth_service.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.UserDTO;
import me.kqlqk.behealthy.tgbot.dto.condition_service.*;
import me.kqlqk.behealthy.tgbot.dto.workout_dto.WorkoutInfoDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "gateway")
public interface GatewayClient {

    @PostMapping("/api/v1/login")
    TokensDTO logInUser(@RequestBody UserDTO userDTO);

    @PostMapping("/api/v1/registration")
    TokensDTO createUser(@RequestBody UserDTO userDTO);

    @GetMapping("/api/v1/users/{id}")
    UserDTO getUser(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/access")
    TokensDTO getNewAccessToken(@RequestBody TokensDTO tokensDTO);

    @PostMapping("/api/v1/update")
    TokensDTO updateTokens(@RequestBody TokensDTO tokensDTO);


    @GetMapping("/api/v1/users/{id}/condition")
    UserConditionDTO getUserCondition(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/condition")
    void createUserCondition(@PathVariable long id, @RequestBody UserConditionDTO userConditionDTO, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/condition/male/fatPercent")
    void createUserConditionWithoutFatPercentMale(@PathVariable long id,
                                                  @RequestBody UserConditionWithoutFatPercentMaleDTO userConditionWithoutFatPercentMaleDTO,
                                                  @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/condition/female/fatPercent")
    void createUserConditionWithoutFatPercentFemale(@PathVariable long id,
                                                    @RequestBody UserConditionWithoutFatPercentFemaleDTO userConditionWithoutFatPercentFemaleDTO,
                                                    @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/condition")
    ResponseEntity<?> updateUserCondition(@PathVariable long id, @RequestBody UserConditionDTO userConditionDTO, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/kcals")
    DailyKcalsDTO getUserDailyKcals(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/food")
    void addDailyAteFoods(@PathVariable long id, @RequestBody DailyAteFoodDTO dailyAteFoodDTO, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/food")
    List<DailyAteFoodDTO> getDailyAteFoods(@PathVariable long id, @RequestHeader String authorization);

    @DeleteMapping("/api/v1/users/{id}/food")
    void deleteDailyAteFood(@PathVariable long id, @RequestParam long productId, @RequestHeader String authorization);


    @PostMapping("/api/v1/users/{id}/workout")
    void createWorkout(@PathVariable long id, @RequestBody WorkoutInfoDTO workoutInfoDTO, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/workout")
    List<WorkoutInfoDTO> getWorkout(@PathVariable long id, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/workout")
    void updateWorkout(@PathVariable long id, @RequestBody WorkoutInfoDTO workoutInfoDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/workout/alternative")
    void updateWorkoutWithAlternativeExercise(@PathVariable long id, @RequestParam String exercise, @RequestHeader String authorization);
}
