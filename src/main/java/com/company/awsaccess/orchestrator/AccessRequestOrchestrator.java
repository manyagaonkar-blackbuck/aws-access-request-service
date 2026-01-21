package com.company.awsaccess.orchestrator;

import com.company.awsaccess.llm.client.LlmClient;
import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import com.company.awsaccess.llm.dto.LlmInterpretResponse;
import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class AccessRequestOrchestrator {

    private final LlmClient llmClient;
    private final AccessRequestService accessRequestService;

    public AccessRequestOrchestrator(
            LlmClient llmClient,
            AccessRequestService accessRequestService) {
        this.llmClient = llmClient;
        this.accessRequestService = accessRequestService;
    }

    public Object handleInitialRequest(LlmInterpretRequest request) {

        String correlationId = UUID.randomUUID().toString();

        LlmInterpretResponse response =
                llmClient.interpret(request, correlationId);

        if (Boolean.TRUE.equals(response.getNeedFollowup())) {
            return response;
        }

        // 🔒 SAFETY VALIDATION — CONTRACT ENFORCEMENT
        if (response.getRequesterEmail() == null ||
            response.getAwsAccount() == null ||
            response.getServices() == null ||
            response.getResourceArns() == null ||
            response.getDurationHours() == null) {

            throw new IllegalStateException("LLM returned incomplete data");
        }

        CreateAccessRequestDto dto = new CreateAccessRequestDto();
        dto.setRequesterEmail(response.getRequesterEmail());
        dto.setAwsAccount(response.getAwsAccount());
        dto.setReason(response.getReason());
        dto.setServices(response.getServices().toString());
        dto.setResourceArns(response.getResourceArns().toString());
        dto.setDurationHours(response.getDurationHours());

        AccessRequest saved =
                accessRequestService.createAccessRequest(dto);

        return saved;
    }
}
