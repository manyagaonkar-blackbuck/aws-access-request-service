package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AccessRequestService {

    private final AccessRequestRepository repository;

    public AccessRequestService(AccessRequestRepository repository) {
        this.repository = repository;
    }

    public AccessRequest createAccessRequest(AccessRequest request) {
        return repository.save(request);
    }

    public AccessRequest getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new IllegalStateException("Request not found"));
    }

    public AccessRequest approveByManager(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.CREATED) {
            throw new IllegalStateException("Manager approval not allowed");
        }

        request.setStatus(AccessRequestStatus.MANAGER_APPROVED);
        request.setApprovedByManager("manager@company.com");
        request.setApprovedAtManager(LocalDateTime.now());

        return repository.save(request);
    }

    public AccessRequest rejectByManager(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.MANAGER_REJECTED);
        return repository.save(request);
    }

    public AccessRequest approveByDevOps(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.MANAGER_APPROVED) {
            throw new IllegalStateException("DevOps approval not allowed");
        }

        request.setStatus(AccessRequestStatus.DEVOPS_APPROVED);
        request.setApprovedByDevOps("devops@company.com");
        request.setApprovedAtDevOps(LocalDateTime.now());

        return repository.save(request);
    }

    public AccessRequest rejectByDevOps(Long id) {
        AccessRequest request = getById(id);
        request.setStatus(AccessRequestStatus.DEVOPS_REJECTED);
        return repository.save(request);
    }
}
