package me.kqlqk.behealthy.tgbot.feign;

import me.kqlqk.behealthy.tgbot.dto.ValidateDTO;
import me.kqlqk.behealthy.tgbot.dto.auth_service.*;
import me.kqlqk.behealthy.tgbot.dto.user_condition_service.*;
import me.kqlqk.behealthy.tgbot.dto.workout_service.*;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@FeignClient(name = "gateway")
public interface GatewayClient {

    @PostMapping("/api/v1/login")
    TokensDTO logInUser(@RequestBody LoginDTO loginDTO);

    @PostMapping("/api/v1/registration")
    TokensDTO createUser(@RequestBody RegistrationDTO registrationDTO);

    @GetMapping("/api/v1/users/{id}")
    GetUserDTO getUser(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/access")
    AccessTokenDTO getNewAccessToken(@RequestBody RefreshTokenDTO refreshTokenDTO);

    @PostMapping("/api/v1/refresh/validate")
    ValidateDTO validateRefreshToken(@RequestBody RefreshTokenDTO refreshTokenDTO);


    @GetMapping("/api/v1/users/{id}/condition")
    GetUserConditionDTO getUserCondition(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/condition")
    void createUserCondition(@PathVariable long id, @RequestBody AddUpdateUserConditionDTO addConditionDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/condition")
    void updateUserCondition(@PathVariable long id, @RequestBody AddUpdateUserConditionDTO updateConditionDTO, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/food")
    List<GetDailyAteFoodDTO> getAllDailyAteFoods(@PathVariable long id, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/food")
    GetDailyAteFoodDTO getSpecifiedDailyAteFoods(@PathVariable long id, @RequestParam String productName, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/food")
    void saveDailyAteFood(@PathVariable long id, @RequestBody AddDailyAteFoodDTO addDailyAteFoodDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/food")
    void updateDailyAteFood(@PathVariable long id, @RequestBody AddDailyAteFoodDTO addDailyAteFoodDTO, @RequestHeader String authorization);

    @DeleteMapping("/api/v1/users/{id}/food")
    void deleteDailyAteFood(@PathVariable long id, @RequestParam String productName, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/kcal")
    GetUserKcalDTO getUserKcal(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/kcal")
    void addUserKcal(@PathVariable long id, @RequestBody AddUpdateUserKcalDTO addUserKcalDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/kcal")
    void updateUserKcal(@PathVariable long id, @RequestBody AddUpdateUserKcalDTO updateUserKcalDTO, @RequestHeader String authorization);


    @GetMapping("/api/v1/users/{id}/workout")
    List<GetWorkoutInfoDTO> getWorkoutInfos(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/workout")
    void createWorkoutInfos(@PathVariable long id, @RequestBody AddUpdateWorkoutInfoDTO addWorkoutInfoDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/workout")
    void updateWorkoutInfos(@PathVariable long id, @RequestBody AddUpdateWorkoutInfoDTO updateWorkoutInfoDTO, @RequestHeader String authorization);

    @PutMapping("/api/v1/users/{id}/workout/alternative")
    void updateWorkoutWithAlternativeExercise(@PathVariable long id, @RequestParam String exerciseName, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/exercises")
    GetExerciseDTO getExercisesByName(@PathVariable long id, @RequestParam String name, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/exercises")
    List<GetExerciseDTO> getExercisesByMuscleGroup(@PathVariable long id, @RequestParam String muscleGroup, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/workout/user")
    List<GetUserWorkoutDTO> getUserWorkout(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/workout/user")
    void addExerciseToUserWorkout(@PathVariable long id, @RequestBody AddUserWorkoutDTO addUserWorkoutDTO, @RequestHeader String authorization);

    @DeleteMapping("/api/v1/users/{id}/workout/user")
    void removeExercise(@PathVariable long id, @RequestParam String exerciseName, @RequestHeader String authorization);


    @GetMapping("/api/v1/users/{id}/photo")
    GetEncodedPhoto getUserEncodedPhotoByDate(@PathVariable long id, @RequestParam String date, @RequestHeader String authorization);

    @GetMapping("/api/v1/users/{id}/photo/all")
    List<FullUserPhotoDTO> getAllUserPhotosAndFiles(@PathVariable long id, @RequestHeader String authorization);

    @PostMapping("/api/v1/users/{id}/photo")
    void saveUserPhoto(@PathVariable long id, @RequestBody AddUserPhotoDTO addUserPhotoDTO, @RequestHeader String authorization);

}
