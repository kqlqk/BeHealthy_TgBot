package me.kqlqk.behealthy.tgbot.feign;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Response;
import feign.codec.ErrorDecoder;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

@Component
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
        else if (info.get("error") != null) {
            errorMessage = info.get("error");
        }
        else {
            errorMessage = "No details about exception";
        }

        return new RuntimeException(errorMessage);
    }
}

