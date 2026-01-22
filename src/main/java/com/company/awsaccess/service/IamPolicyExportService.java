package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class IamPolicyExportService {

    private final IamPolicyService iamPolicyService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public IamPolicyExportService(IamPolicyService iamPolicyService) {
        this.iamPolicyService = iamPolicyService;
    }

    public String generatePolicyJson(AccessRequest request) {

        try {
            Map<String, Object> policy =
                    iamPolicyService.generatePolicy(request);

            return objectMapper
                    .writerWithDefaultPrettyPrinter()
                    .writeValueAsString(policy);

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate policy JSON", e);
        }
    }
}
