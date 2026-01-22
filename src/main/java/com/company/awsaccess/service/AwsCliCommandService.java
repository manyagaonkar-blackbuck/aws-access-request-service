package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class AwsCliCommandService {

    private final IamPolicyService iamPolicyService;

    public AwsCliCommandService(IamPolicyService iamPolicyService) {
        this.iamPolicyService = iamPolicyService;
    }

    public String generateCreatePolicyCommand(AccessRequest request) {

        Map<String, Object> policy =
                iamPolicyService.generatePolicy(request);

        String policyJson =
                policy.toString().replace("=", ":");

        return "aws iam create-policy " +
               "--policy-name access-request-" + request.getId() + " " +
               "--policy-document '" + policyJson + "'";
    }
}
