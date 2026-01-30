package com.company.awsaccess.service;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.model.AccessRequest;

import java.util.List;

public interface AccessRequestService {

    // ✅ DTO-BASED CREATE (SOURCE OF TRUTH)
    AccessRequest create(CreateAccessRequestDto dto);

    List<AccessRequest> getAll();

    AccessRequest getById(Long id);

    AccessRequest approveByManager(Long id);

    AccessRequest rejectByManager(Long id);

    AccessRequest approveByDevOps(Long id);

    AccessRequest rejectByDevOps(Long id);
}
