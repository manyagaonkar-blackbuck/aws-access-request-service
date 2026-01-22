package com.company.awsaccess.controller;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import com.company.awsaccess.service.AwsCliCommandService;
import com.company.awsaccess.service.IamPolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;
    private final IamPolicyService iamPolicyService;
    private final AwsCliCommandService awsCliCommandService;

    public AccessRequestController(
            AccessRequestService service,
            IamPolicyService iamPolicyService,
            AwsCliCommandService awsCliCommandService) {

        this.service = service;
        this.iamPolicyService = iamPolicyService;
        this.awsCliCommandService = awsCliCommandService;
    }

    @PostMapping
    public AccessRequest create(@RequestBody AccessRequest request) {
        return service.createAccessRequest(request);
    }

    @PostMapping("/{id}/manager/approve")
    public AccessRequest managerApprove(@PathVariable Long id) {
        return service.approveByManager(id);
    }

    @PostMapping("/{id}/manager/reject")
    public AccessRequest managerReject(@PathVariable Long id) {
        return service.rejectByManager(id);
    }

    @PostMapping("/{id}/devops/approve")
    public AccessRequest devopsApprove(@PathVariable Long id) {
        return service.approveByDevOps(id);
    }

    @PostMapping("/{id}/devops/reject")
    public AccessRequest devopsReject(@PathVariable Long id) {
        return service.rejectByDevOps(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> status(@PathVariable Long id) {
        return Map.of("status", service.getById(id).getStatus().name());
    }

    @GetMapping("/{id}/iam-policy")
    public Map<String, Object> iamPolicy(@PathVariable Long id) {
        AccessRequest request = service.getById(id);
        return iamPolicyService.generatePolicy(request);
    }

    @GetMapping("/{id}/aws-cli")
    public Map<String, String> awsCli(@PathVariable Long id) {
        AccessRequest request = service.getById(id);
        return Map.of(
                "command",
                awsCliCommandService.generateCreatePolicyCommand(request)
        );
    }
}
