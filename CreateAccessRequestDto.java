package com.company.awsaccess.dto.request;

import java.util.List;

public class CreateAccessRequestDto {

    private String requesterEmail;
    private String awsAccount;
    private String reason;

    // ✅ MUST BE LIST
    private List<String> services;

    // ✅ MUST BE LIST
    private List<String> resourceArns;

    private Integer durationHours;

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
ss
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
