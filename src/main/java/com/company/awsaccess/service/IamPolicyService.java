package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.RequestStatus;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class IamPolicyService {

    public Map<String, Object> generatePolicy(AccessRequest request) {

        if (request.getStatus() != RequestStatus.DEVOPS_APPROVED) {
            throw new IllegalStateException(
                    "IAM policy can be generated only after DevOps approval"
            );
        }

        Map<String, Object> policy = new LinkedHashMap<>();
        policy.put("Version", "2012-10-17");

        Map<String, Object> statement = new LinkedHashMap<>();
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

        if (services.contains("EC2")) {
            actions.add("ec2:DescribeInstances");
            actions.add("ec2:StartInstances");
            actions.add("ec2:StopInstances");
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
