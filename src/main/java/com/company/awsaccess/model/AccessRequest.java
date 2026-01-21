package com.company.awsaccess.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String requesterEmail;

    @Column(nullable = false)
    private String awsAccount;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String services;

    @Column(columnDefinition = "TEXT")
    private String resourceArns;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RequestStatus status;

    private Integer durationHours;

    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.durationHours = 24;
        this.status = RequestStatus.CREATED;
    }

    public Long getId() {
        return id;
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

    public RequestStatus getStatus() {
        return status;
    }

    // 🔥 THIS WAS MISSING
    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
