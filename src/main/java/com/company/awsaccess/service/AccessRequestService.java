package com.company.awsaccess.service;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.RequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

@Service
public class AccessRequestService {

    private final AccessRequestRepository repository;

    public AccessRequestService(AccessRequestRepository repository) {
        this.repository = repository;
    }

    public AccessRequest createAccessRequest(CreateAccessRequestDto dto) {

        AccessRequest request = new AccessRequest();
        request.setRequesterEmail(dto.getRequesterEmail());
        request.setAwsAccount(dto.getAwsAccount());
        request.setReason(dto.getReason());
        request.setServices(dto.getServices());
        request.setResourceArns(dto.getResourceArns());

        return repository.save(request);
    }

    public AccessRequest getById(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));
    }

    public AccessRequest approveByManager(Long id) {

        AccessRequest request = getById(id);

        if (request.getStatus() != RequestStatus.CREATED) {
            throw new RuntimeException("Manager approval not allowed in state: " + request.getStatus());
        }

        request.setStatus(RequestStatus.MANAGER_APPROVED);
        return repository.save(request);
    }

    public AccessRequest rejectByManager(Long id) {

        AccessRequest request = getById(id);

        if (request.getStatus() != RequestStatus.CREATED) {
            throw new RuntimeException("Manager rejection not allowed in state: " + request.getStatus());
        }

        request.setStatus(RequestStatus.MANAGER_REJECTED);
        return repository.save(request);
    }

    public AccessRequest approveByDevOps(Long id) {

        AccessRequest request = getById(id);

        if (request.getStatus() != RequestStatus.MANAGER_APPROVED) {
            throw new RuntimeException("DevOps approval not allowed in state: " + request.getStatus());
        }

        request.setStatus(RequestStatus.DEVOPS_APPROVED);
        return repository.save(request);
    }

    public AccessRequest rejectByDevOps(Long id) {

        AccessRequest request = getById(id);

        if (request.getStatus() != RequestStatus.MANAGER_APPROVED) {
            throw new RuntimeException("DevOps rejection not allowed in state: " + request.getStatus());
        }

        request.setStatus(RequestStatus.DEVOPS_REJECTED);
        return repository.save(request);
    }
}
