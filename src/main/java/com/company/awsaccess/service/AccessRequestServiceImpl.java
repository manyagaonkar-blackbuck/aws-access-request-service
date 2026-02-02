package com.company.awsaccess.service;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class AccessRequestServiceImpl implements AccessRequestService {

    private final AccessRequestRepository repository;

    public AccessRequestServiceImpl(AccessRequestRepository repository) {
        this.repository = repository;
    }

    @Override
    public AccessRequest create(CreateAccessRequestDto dto) {

        // ===============================
        // 🛠️ NULL-SAFE DEFAULTS (FIX)
        // ===============================
        if (dto.getServices() == null) {
            dto.setServices(List.of());
        }

        if (dto.getResourceArns() == null) {
            dto.setResourceArns(List.of());
        }

        if (dto.getDurationHours() == null) {
            dto.setDurationHours(4); // default duration
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

        // ✅ CORRECT ENUM VALUE (FROM YOUR ENUM)
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
