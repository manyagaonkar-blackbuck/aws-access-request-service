package com.company.awsaccess.controller;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;

    public AccessRequestController(AccessRequestService service) {
        this.service = service;
    }

    @PostMapping
    public AccessRequestResponseDto create(@RequestBody CreateAccessRequestDto dto) {

        AccessRequest saved = service.createAccessRequest(dto);

        return toResponse(saved);
    }

    @GetMapping("/{id}")
    public AccessRequestResponseDto get(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping("/{id}/manager/approve")
    public AccessRequestResponseDto managerApprove(@PathVariable Long id) {
        return toResponse(service.approveByManager(id));
    }

    @PostMapping("/{id}/manager/reject")
    public AccessRequestResponseDto managerReject(@PathVariable Long id) {
        return toResponse(service.rejectByManager(id));
    }

    @PostMapping("/{id}/devops/approve")
    public AccessRequestResponseDto devopsApprove(@PathVariable Long id) {
        return toResponse(service.approveByDevOps(id));
    }

    @PostMapping("/{id}/devops/reject")
    public AccessRequestResponseDto devopsReject(@PathVariable Long id) {
        return toResponse(service.rejectByDevOps(id));
    }

    private AccessRequestResponseDto toResponse(AccessRequest request) {
        return new AccessRequestResponseDto(
                request.getId(),
                request.getRequesterEmail(),
                request.getAwsAccount(),
                request.getStatus().name(),
                request.getCreatedAt()
        );
    }
}
