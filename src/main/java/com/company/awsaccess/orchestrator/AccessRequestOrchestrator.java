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
            // frontend must ask follow-up question
            return response;
        }

        // LLM returned complete structured data → create access request
        CreateAccessRequestDto dto = mapToCreateDto(response);

        AccessRequest saved =
                accessRequestService.createAccessRequest(dto);

        return saved;
    }

    private CreateAccessRequestDto mapToCreateDto(
            LlmInterpretResponse llm) {

        CreateAccessRequestDto dto = new CreateAccessRequestDto();

        dto.setRequesterEmail(llm.getRequesterEmail());
        dto.setAwsAccount(llm.getAwsAccount());
        dto.setReason(llm.getReason());
        dto.setServices(llm.getServices().toString());
        dto.setResourceArns(llm.getResourceArns().toString());
        dto.setDurationHours(llm.getDurationHours());

        return dto;
    }
}
