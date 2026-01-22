package com.company.awsaccess.controller;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;
    private final IamPolicyService iamPolicyService;
    private final AwsCliCommandService awsCliCommandService;
    private final IamPolicyExportService iamPolicyExportService;

    public AccessRequestController(
            AccessRequestService service,
            IamPolicyService iamPolicyService,
            AwsCliCommandService awsCliCommandService,
            IamPolicyExportService iamPolicyExportService) {

        this.service = service;
        this.iamPolicyService = iamPolicyService;
        this.awsCliCommandService = awsCliCommandService;
        this.iamPolicyExportService = iamPolicyExportService;
    }

    @PostMapping
    public AccessRequestResponseDto create(
            @RequestBody CreateAccessRequestDto dto) {

        return toResponse(service.createAccessRequest(dto));
    }

    @GetMapping("/{id}")
    public AccessRequestResponseDto get(@PathVariable Long id) {
        return toResponse(service.getById(id));
    }

    @PostMapping("/{id}/manager/approve")
    public AccessRequestResponseDto managerApprove(@PathVariable Long id) {
        return toResponse(service.approveByManager(id));
    }

    @PostMapping("/{id}/devops/approve")
    public AccessRequestResponseDto devopsApprove(@PathVariable Long id) {
        return toResponse(service.approveByDevOps(id));
    }

    @GetMapping("/{id}/iam-policy.json")
    public Map<String, Object> getIamPolicy(@PathVariable Long id) {

        AccessRequest request = service.getById(id);
        return iamPolicyService.generatePolicy(request);
    }

    @GetMapping("/{id}/iam-policy/download")
    public ResponseEntity<String> downloadIamPolicy(@PathVariable Long id) {

        AccessRequest request = service.getById(id);
        String json =
                iamPolicyExportService.generatePolicyJson(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=policy-" + id + ".json")
                .contentType(MediaType.APPLICATION_JSON)
                .body(json);
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
