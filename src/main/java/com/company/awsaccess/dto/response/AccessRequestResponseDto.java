package com.company.awsaccess.dto.response;

import java.time.LocalDateTime;

public class AccessRequestResponseDto {

    private Long id;
    private String requesterEmail;
    private String awsAccount;
    private String status;
    private LocalDateTime createdAt;

    public AccessRequestResponseDto(
            Long id,
            String requesterEmail,
            String awsAccount,
            String status,
            LocalDateTime createdAt
    ) {
        this.id = id;
        this.requesterEmail = requesterEmail;
        this.awsAccount = awsAccount;
        this.status = status;
        this.createdAt = createdAt;
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
}
