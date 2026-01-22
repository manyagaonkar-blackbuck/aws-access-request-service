package com.company.awsaccess.controller;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.service.AccessRequestService;
import com.company.awsaccess.service.IamPolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class IamPolicyController {

    private final AccessRequestService service;
    private final IamPolicyService iamPolicyService;

    public IamPolicyController(
            AccessRequestService service,
            IamPolicyService iamPolicyService) {
        this.service = service;
        this.iamPolicyService = iamPolicyService;
    }

    @GetMapping("/{id}/iam-policy")
    public Map<String, Object> getIamPolicy(@PathVariable Long id) {

        AccessRequest request = service.getById(id);

        if (request.getStatus() != AccessRequestStatus.DEVOPS_APPROVED) {
            throw new IllegalStateException(
                    "IAM policy available only after DevOps approval");
        }

        return iamPolicyService.generatePolicy(request);
    }
}
