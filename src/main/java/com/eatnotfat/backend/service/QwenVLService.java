package com.eatnotfat.backend.service;

import com.eatnotfat.backend.config.QwenConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QwenVLService {

    @Autowired
    private QwenConfig qwenConfig;

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String recognizeImage(String imagePath, String prompt) {
        try {
            String apiKey = qwenConfig.getApiKey();
            String endpoint = qwenConfig.getVlEndpoint();
            String model = qwenConfig.getVlModel();

            Map<String, Object> requestBody = buildVLRequest(imagePath, prompt, model);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + apiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    endpoint, HttpMethod.POST, entity, String.class);

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return parseVLResponse(response.getBody());
            }

            return null;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    private Map<String, Object> buildVLRequest(String imagePath, String prompt, String model) {
        Map<String, Object> request = new HashMap<>();
        request.put("model", model);

        Map<String, Object> input = new HashMap<>();
        List<Map<String, Object>> messages = new ArrayList<>();

        Map<String, Object> message = new HashMap<>();
        message.put("role", "user");

        List<Map<String, Object>> contentList = new ArrayList<>();

        Map<String, Object> imageContent = new HashMap<>();
        if (imagePath.startsWith("http://") || imagePath.startsWith("https://")) {
            imageContent.put("image", imagePath);
        } else {
            String base64 = imageToBase64(imagePath);
            if (base64 != null) {
                imageContent.put("image", "data:image/jpeg;base64," + base64);
            }
        }
        contentList.add(imageContent);

        Map<String, Object> textContent = new HashMap<>();
        textContent.put("text", prompt);
        contentList.add(textContent);

        message.put("content", contentList);
        messages.add(message);

        input.put("messages", messages);
        request.put("input", input);

        Map<String, Object> parameters = new HashMap<>();
        parameters.put("max_tokens", qwenConfig.getMaxTokens());
        parameters.put("temperature", qwenConfig.getTemperature());
        request.put("parameters", parameters);

        return request;
    }

    private String parseVLResponse(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode output = root.path("output");
            JsonNode choices = output.path("choices");

            if (choices.isArray() && choices.size() > 0) {
                JsonNode msgNode = choices.get(0).path("message");
                JsonNode content = msgNode.path("content");

                if (content.isArray() && content.size() > 0) {
                    for (JsonNode item : content) {
                        if (item.has("text")) {
                            return item.path("text").asText();
                        }
                    }
                } else if (content.isTextual()) {
                    return content.asText();
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private String imageToBase64(String imagePath) {
        try {
            File file = new File(imagePath);
            if (!file.exists()) {
                return null;
            }
            byte[] fileContent = Files.readAllBytes(file.toPath());
            return Base64.getEncoder().encodeToString(fileContent);
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}
