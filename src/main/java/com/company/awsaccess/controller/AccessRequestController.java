package com.company.awsaccess.controller;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import com.company.awsaccess.service.AwsCliCommandService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;
    private final AwsCliCommandService awsCliCommandService;

    public AccessRequestController(
            AccessRequestService service,
            AwsCliCommandService awsCliCommandService) {
        this.service = service;
        this.awsCliCommandService = awsCliCommandService;
    }

    @PostMapping
    public AccessRequestResponseDto create(
            @RequestBody CreateAccessRequestDto dto) {

        AccessRequest request = service.createAccessRequest(dto);
        return toResponse(request);
    }

    @GetMapping("/{id}")
    public AccessRequestResponseDto getById(@PathVariable Long id) {
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

    @GetMapping("/{id}/aws-cli")
    public Map<String, String> getAwsCli(@PathVariable Long id) {

        AccessRequest request = service.getById(id);

        String command =
                awsCliCommandService.generateCreatePolicyCommand(request);

        return Map.of("command", command);
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
