package com.company.awsaccess.service;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    private final AccessRequestRepository repository;
    private final WebClient webClient;

    public AccessRequestServiceImpl(
            AccessRequestRepository repository,
            WebClient webClient
    ) {
        this.repository = repository;
        this.webClient = webClient;
    }

    @Override
    public AccessRequest create(CreateAccessRequestDto dto) {

        // ===============================
        // 🔗 CALL LLM (SAFE)
        // ===============================
        try {
            Map<String, Object> llmResponse =
                    webClient.post()
                            .uri("http://localhost:9000/api/v1/llm/interpret")
                            .bodyValue(Map.of(
                                    "requesterEmail", dto.getRequesterEmail(),
                                    "awsAccount", dto.getAwsAccount(),
                                    "reason", dto.getReason()
                            ))
                            .retrieve()
                            .bodyToMono(Map.class)
                            .block();

            if (llmResponse != null) {
                if (dto.getServices() == null) {
                    dto.setServices((List<String>) llmResponse.get("services"));
                }

                if (dto.getResourceArns() == null) {
                    dto.setResourceArns((List<String>) llmResponse.get("resourceArns"));
                }

                if (dto.getDurationHours() == null) {
                    Object duration = llmResponse.get("durationHours");
                    if (duration instanceof Integer) {
                        dto.setDurationHours((Integer) duration);
                    }
                }

                System.out.println("LLM RESPONSE: " + llmResponse);
            }

        } catch (Exception e) {
            // 🚨 LLM FAILURE SHOULD NEVER BREAK BACKEND
            System.out.println("LLM call failed, using defaults");
        }

        // ===============================
        // 🛠️ FALLBACK DEFAULTS
        // ===============================
        if (dto.getServices() == null) {
            dto.setServices(List.of());
        }

        if (dto.getResourceArns() == null) {
            dto.setResourceArns(List.of());
        }

        if (dto.getDurationHours() == null) {
            dto.setDurationHours(4);
        }

        // ===============================
        // CREATE ENTITY
        // ===============================
        AccessRequest request = new AccessRequest();

        request.setRequesterEmail(dto.getRequesterEmail());
        request.setAwsAccount(dto.getAwsAccount());
        request.setReason(dto.getReason());

        request.setServices(String.join(",", dto.getServices()));
        request.setResourceArns(String.join(",", dto.getResourceArns()));
        request.setDurationHours(dto.getDurationHours());

        request.setStatus(AccessRequestStatus.CREATED);

        return repository.save(request);
    }

    @Override
    public List<AccessRequest> getAll() {
        return repository.findAll();
    }

    @Override
    public AccessRequest approveByManager(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.MANAGER_APPROVED);
        return repository.save(request);
    }

    @Override
    public AccessRequest rejectByManager(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.MANAGER_REJECTED);
        return repository.save(request);
    }

    @Override
    public AccessRequest approveByDevOps(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.DEVOPS_APPROVED);
        return repository.save(request);
    }

    @Override
    public AccessRequest rejectByDevOps(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.DEVOPS_REJECTED);
        return repository.save(request);
    }

    @Override
    public AccessRequest getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Access request not found"));
    }
}
