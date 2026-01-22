package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IamPolicyService {

    public Map<String, Object> generatePolicy(AccessRequest request) {

        if (request.getStatus() != AccessRequestStatus.DEVOPS_APPROVED) {
            throw new IllegalStateException("IAM policy available only after DevOps approval");
        }

        if (request.isExpired()) {
            throw new IllegalStateException("Access request has expired");
        }

        Map<String, Object> policy = new HashMap<>();
        policy.put("Version", "2012-10-17");

        Map<String, Object> statement = new HashMap<>();
        statement.put("Effect", "Allow");
        statement.put("Action", mapActions(request.getServices()));
        statement.put("Resource", parseResources(request.getResourceArns()));

        policy.put("Statement", List.of(statement));
        return policy;
    }

    private List<String> mapActions(String services) {
        List<String> actions = new ArrayList<>();

        if (services.contains("S3")) {
            actions.add("s3:PutObject");
            actions.add("s3:GetObject");
            actions.add("s3:ListBucket");
        }
        return actions;
    }

    private List<String> parseResources(String resourceArns) {
        return Arrays.asList(
                resourceArns.replace("[", "")
                        .replace("]", "")
                        .replace("\"", "")
                        .split(",")
        );
    }
}
