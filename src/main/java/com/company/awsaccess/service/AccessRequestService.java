package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

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
        AccessRequest request =
                repository.findById(id)
                        .orElseThrow(() -> new IllegalArgumentException("Request not found"));

        if (request.isExpired()) {
            request.setStatus(AccessRequestStatus.EXPIRED);
            repository.save(request);
        }
        return request;
    }

    public AccessRequest approveByManager(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.CREATED) {
            throw new IllegalStateException("Manager approval not allowed");
        }

        request.setStatus(AccessRequestStatus.MANAGER_APPROVED);
        return repository.save(request);
    }

    public AccessRequest rejectByManager(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.CREATED) {
            throw new IllegalStateException("Manager rejection not allowed");
        }

        request.setStatus(AccessRequestStatus.MANAGER_REJECTED);
        return repository.save(request);
    }

    public AccessRequest approveByDevOps(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.MANAGER_APPROVED) {
            throw new IllegalStateException("DevOps approval not allowed");
        }

        request.setStatus(AccessRequestStatus.DEVOPS_APPROVED);
        return repository.save(request);
    }

    public AccessRequest rejectByDevOps(Long id) {
        AccessRequest request = getById(id);

        if (request.getStatus() != AccessRequestStatus.MANAGER_APPROVED) {
            throw new IllegalStateException("DevOps rejection not allowed");
        }

        request.setStatus(AccessRequestStatus.DEVOPS_REJECTED);
        return repository.save(request);
    }
}
