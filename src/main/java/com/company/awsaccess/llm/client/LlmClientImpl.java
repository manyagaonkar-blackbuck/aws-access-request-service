package com.company.awsaccess.llm.client;

import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import com.company.awsaccess.llm.dto.LlmInterpretResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class LlmClientImpl implements LlmClient {

    private final WebClient webClient;

    public LlmClientImpl(WebClient.Builder builder) {
        this.webClient = builder
                .baseUrl("http://localhost:9000")
                .build();
    }

    @Override
    public LlmInterpretResponse interpret(LlmInterpretRequest req) {

        // 🔑 REQUIRED by FastAPI
        if (req.getRequestId() == null || req.getRequestId().isBlank()) {
            req.setRequestId("REQ-" + System.currentTimeMillis());
        }

        return webClient.post()
                .uri("/api/v1/llm/interpret")
                .bodyValue(req)
                .retrieve()
                .bodyToMono(LlmInterpretResponse.class)
                .block();
    }
}
