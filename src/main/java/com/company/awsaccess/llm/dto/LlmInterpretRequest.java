package com.company.awsaccess.llm.dto;

import java.util.List;
import java.util.Map;

public class LlmInterpretRequest {

    private String requestId;
    private String requesterEmail;
    private String awsAccount;
    private String reason;
    private Integer durationHours;
    private List<String> allowedServices;
    private Map<String, List<String>> allowedActionGroups;

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public void setAwsAccount(String awsAccount) {
        this.awsAccount = awsAccount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public List<String> getAllowedServices() {
        return allowedServices;
    }

    public void setAllowedServices(List<String> allowedServices) {
        this.allowedServices = allowedServices;
    }

    public Map<String, List<String>> getAllowedActionGroups() {
        return allowedActionGroups;
    }

    public void setAllowedActionGroups(Map<String, List<String>> allowedActionGroups) {
        this.allowedActionGroups = allowedActionGroups;
    }
}
