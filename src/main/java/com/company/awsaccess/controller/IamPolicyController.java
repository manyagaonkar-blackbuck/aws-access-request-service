package com.company.awsaccess.controller;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.RequestStatus;
import com.company.awsaccess.service.AccessRequestService;
import com.company.awsaccess.service.IamPolicyService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class IamPolicyController {

    private final AccessRequestService accessRequestService;
    private final IamPolicyService iamPolicyService;

    public IamPolicyController(
            AccessRequestService accessRequestService,
            IamPolicyService iamPolicyService) {
        this.accessRequestService = accessRequestService;
        this.iamPolicyService = iamPolicyService;
    }

    @GetMapping("/{id}/iam-policy")
    public Map<String, Object> getIamPolicy(@PathVariable Long id) {

        AccessRequest request = accessRequestService.getById(id);

        if (request.getStatus() != RequestStatus.DEVOPS_APPROVED) {
            throw new IllegalStateException(
                    "IAM policy can be generated only after DevOps approval"
            );
        }

        return iamPolicyService.generatePolicy(request);
    }
}
