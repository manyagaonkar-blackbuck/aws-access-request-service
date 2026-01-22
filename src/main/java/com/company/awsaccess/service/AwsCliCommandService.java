package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import org.springframework.stereotype.Service;

@Service
public class AwsCliCommandService {

    public String generateCreatePolicyCommand(AccessRequest request) {

        String policyName =
                "access-request-" + request.getId();

        return "aws iam create-policy " +
                "--policy-name " + policyName + " " +
                "--policy-document file://policy-" +
                request.getId() + ".json";
    }
}
