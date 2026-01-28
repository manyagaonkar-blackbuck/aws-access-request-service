package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;

import java.util.List;

public interface AccessRequestService {

    AccessRequest createAccessRequest(AccessRequest request);

    AccessRequest approveByManager(Long id);

    AccessRequest rejectByManager(Long id);

    AccessRequest approveByDevOps(Long id);

    AccessRequest rejectByDevOps(Long id);

    AccessRequest getById(Long id);

    // ✅ ADD THIS
    List<AccessRequest> getAll();
}
