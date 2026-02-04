package com.company.awsaccess.dto.response;

import java.time.LocalDateTime;

public class AccessRequestResponseDto {

    private Long id;
    private String requesterEmail;
    private String awsAccount;
    private String status;
    private LocalDateTime createdAt;
    private String services;

    public AccessRequestResponseDto(
            Long id,
            String requesterEmail,
            String awsAccount,
            String status,
            LocalDateTime createdAt,
            String services
    ) {
        this.id = id;
        this.requesterEmail = requesterEmail;
        this.awsAccount = awsAccount;
        this.status = status;
        this.createdAt = createdAt;
        this.services = services;
    }

    public Long getId() {
        return id;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public String getServices() {
        return services;
    }
}
