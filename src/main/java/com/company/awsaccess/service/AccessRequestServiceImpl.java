package com.company.awsaccess.service;

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
    public AccessRequest createAccessRequest(AccessRequest request) {
        return repository.save(request);
    }

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

    @Override
    public AccessRequest getById(Long id) {
        return repository.findById(id).orElseThrow();
    }

    // ✅ REQUIRED BY INTERFACE
    @Override
    public List<AccessRequest> getAll() {
        return repository.findAll();
    }
}
