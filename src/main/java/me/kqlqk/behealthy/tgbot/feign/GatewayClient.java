package me.kqlqk.behealthy.tgbot.feign;

import me.kqlqk.behealthy.tgbot.dto.TokensDTO;
import me.kqlqk.behealthy.tgbot.dto.UserConditionDTO;
import me.kqlqk.behealthy.tgbot.dto.UserDTO;
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
}
