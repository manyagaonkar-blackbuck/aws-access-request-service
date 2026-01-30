package com.company.awsaccess.service;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    private final AccessRequestRepository repository;

    public AccessRequestServiceImpl(AccessRequestRepository repository) {
        this.repository = repository;
    }

    // ===== CREATE =====
    @Override
    public AccessRequest create(CreateAccessRequestDto dto) {
        AccessRequest req = new AccessRequest();

        req.setRequesterEmail(dto.getRequesterEmail());
        req.setAwsAccount(dto.getAwsAccount());
        req.setReason(dto.getReason());

        // frontend sends LIST, DB stores STRING
        req.setServices(String.join(",", dto.getServices()));
        req.setResourceArns(String.join(",", dto.getResourceArns()));

        req.setDurationHours(dto.getDurationHours());
        req.setStatus(AccessRequestStatus.CREATED);
        req.setExpiresAt(
            LocalDateTime.now().plusHours(dto.getDurationHours())
        );

        return repository.save(req);
    }

    // ===== READ =====
    @Override
    public List<AccessRequest> getAll() {
        return repository.findAll();
    }

    @Override
    public AccessRequest getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    // ===== MANAGER ACTIONS =====
    @Override
    public AccessRequest approveByManager(Long id) {
        AccessRequest req = repository.findById(id).orElseThrow();
        req.setStatus(AccessRequestStatus.MANAGER_APPROVED);
        return repository.save(req);
    }

    @Override
    public AccessRequest rejectByManager(Long id) {
        AccessRequest req = repository.findById(id).orElseThrow();
        req.setStatus(AccessRequestStatus.MANAGER_REJECTED);
        return repository.save(req);
    }

    // ===== DEVOPS ACTIONS =====
    @Override
    public AccessRequest approveByDevOps(Long id) {
        AccessRequest req = repository.findById(id).orElseThrow();
        req.setStatus(AccessRequestStatus.DEVOPS_APPROVED);
        return repository.save(req);
    }

    @Override
    public AccessRequest rejectByDevOps(Long id) {
        AccessRequest req = repository.findById(id).orElseThrow();
        req.setStatus(AccessRequestStatus.DEVOPS_REJECTED);
        return repository.save(req);
    }
}
