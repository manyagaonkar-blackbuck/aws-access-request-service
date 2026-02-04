package com.company.awsaccess.service;
import com.company.awsaccess.model.AccessRequestStatus;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.llm.client.LlmClient;
import com.company.awsaccess.llm.dto.LlmInterpretRequest;
import com.company.awsaccess.llm.dto.LlmInterpretResponse;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    @Autowired
    private AccessRequestRepository repository;

    @Autowired
    private LlmClient llmClient;

    // ✅ REQUIRED BY INTERFACE
    @Override
    public AccessRequest create(CreateAccessRequestDto dto) {

        // DTO → Entity
        AccessRequest req = new AccessRequest();
        req.setRequesterEmail(dto.getRequesterEmail());
        req.setAwsAccount(dto.getAwsAccount());
        req.setReason(dto.getReason());

        // LLM request
        LlmInterpretRequest llmReq = new LlmInterpretRequest();
        llmReq.setRequesterEmail(req.getRequesterEmail());
        llmReq.setAwsAccount(req.getAwsAccount());
        llmReq.setReason(req.getReason());

        // Call LLM
        LlmInterpretResponse llmResp = llmClient.interpret(llmReq);

        if (Boolean.TRUE.equals(llmResp.getNeedFollowup())) {
            req.setStatus(AccessRequestStatus.CREATED);
            return repository.save(req);
        }

        if (llmResp.getServices() != null) {
            req.setServices(String.join(",", llmResp.getServices()));
        }

        if (llmResp.getResourceArns() != null) {
            req.setResourceArns(String.join(",", llmResp.getResourceArns()));
        }

        req.setDurationHours(llmResp.getDurationHours());
        req.setStatus(AccessRequestStatus.CREATED);

        return repository.save(req);
    }

    // ---- STUB METHODS (required by interface) ----

    @Override
    public List<AccessRequest> getAll() {
        return repository.findAll();
    }

    @Override
    public AccessRequest getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    @Override
    public AccessRequest approveByManager(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccessRequest rejectByManager(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccessRequest approveByDevOps(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    @Override
    public AccessRequest rejectByDevOps(Long id) {
        throw new UnsupportedOperationException("Not implemented yet");
    }
}
