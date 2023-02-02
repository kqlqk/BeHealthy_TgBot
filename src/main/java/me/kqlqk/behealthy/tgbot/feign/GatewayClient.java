package me.kqlqk.behealthy.tgbot.feign;

import me.kqlqk.behealthy.tgbot.dto.authService.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.authService.UserDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionWithoutFatPercentFemaleDTO;
import me.kqlqk.behealthy.tgbot.dto.conditionService.UserConditionWithoutFatPercentMaleDTO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

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
}
