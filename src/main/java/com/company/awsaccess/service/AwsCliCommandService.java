package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import org.springframework.stereotype.Service;

@Service
public class AwsCliCommandService {

    public String generateCreatePolicyCommand(AccessRequest request) {

        if (request.getStatus() != AccessRequestStatus.DEVOPS_APPROVED) {
            throw new IllegalStateException("AWS CLI command available only after DevOps approval");
        }

        if (request.isExpired()) {
            throw new IllegalStateException("Access request has expired");
        }

        return "aws iam create-policy "
                + "--policy-name access-" + request.getId() + " "
                + "--policy-document file://policy-" + request.getId() + ".json";
    }
}
