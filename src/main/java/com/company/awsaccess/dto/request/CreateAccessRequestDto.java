package com.company.awsaccess.dto.request;

public class CreateAccessRequestDto {

    private String requesterEmail;
    private String awsAccount;
    private String reason;
    private String services;
    private String resourceArns;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getServices() {
        return services;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public String getResourceArns() {
        return resourceArns;
    }

    public void setResourceArns(String resourceArns) {
        this.resourceArns = resourceArns;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }
}
