package com.company.awsaccess.llm.client;

import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import com.company.awsaccess.llm.dto.LlmInterpretResponse;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class LlmClient {

    private static final String BASE_URL = "http://localhost:9000";

    private final RestTemplate restTemplate = new RestTemplate();

    public LlmInterpretResponse interpret(LlmInterpretRequest request, String correlationId) {

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("X-Correlation-Id", correlationId);

        HttpEntity<LlmInterpretRequest> entity =
                new HttpEntity<>(request, headers);

        return restTemplate.postForObject(
                BASE_URL + "/api/v1/llm/interpret",
                entity,
                LlmInterpretResponse.class
        );
    }
}
