package com.company.awsaccess.llm.dto;

import java.util.List;

public class LlmInterpretRequest {

    private String requestId;
    private String requesterEmail;
    private String awsAccount;
    private String reason;
    private List<String> services;
    private List<String> actionGroups;
    private List<String> resourceArns;
    private Integer durationHours;

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

    public List<String> getServices() {
        return services;
    }

    public void setServices(List<String> services) {
        this.services = services;
    }

    public List<String> getActionGroups() {
        return actionGroups;
    }

    public void setActionGroups(List<String> actionGroups) {
        this.actionGroups = actionGroups;
    }

    public List<String> getResourceArns() {
        return resourceArns;
    }

    public void setResourceArns(List<String> resourceArns) {
        this.resourceArns = resourceArns;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }
}