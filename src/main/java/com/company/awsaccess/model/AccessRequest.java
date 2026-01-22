package com.company.awsaccess.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "access_requests")
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String requesterEmail;
    private String awsAccount;

    @Column(columnDefinition = "TEXT")
    private String reason;

    @Column(columnDefinition = "TEXT")
    private String services;

    @Column(columnDefinition = "TEXT")
    private String resourceArns;

    private Integer durationHours;

    @Enumerated(EnumType.STRING)
    private AccessRequestStatus status;

    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;

    private String approvedByManager;
    private String approvedByDevOps;

    private LocalDateTime approvedAtManager;
    private LocalDateTime approvedAtDevOps;

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (durationHours != null) {
            this.expiresAt = createdAt.plusHours(durationHours);
        }
        this.status = AccessRequestStatus.CREATED;
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // ===== GETTERS =====

    public Long getId() {
        return id;
    }

    public String getRequesterEmail() {
        return requesterEmail;
    }

    public String getAwsAccount() {
        return awsAccount;
    }

    public String getReason() {
        return reason;
    }

    public String getServices() {
        return services;
    }

    public String getResourceArns() {
        return resourceArns;
    }

    public Integer getDurationHours() {
        return durationHours;
    }

    public AccessRequestStatus getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public String getApprovedByManager() {
        return approvedByManager;
    }

    public String getApprovedByDevOps() {
        return approvedByDevOps;
    }

    public LocalDateTime getApprovedAtManager() {
        return approvedAtManager;
    }

    public LocalDateTime getApprovedAtDevOps() {
        return approvedAtDevOps;
    }

    // ===== SETTERS =====

    public void setRequesterEmail(String requesterEmail) {
        this.requesterEmail = requesterEmail;
    }

    public void setAwsAccount(String awsAccount) {
        this.awsAccount = awsAccount;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public void setServices(String services) {
        this.services = services;
    }

    public void setResourceArns(String resourceArns) {
        this.resourceArns = resourceArns;
    }

    public void setDurationHours(Integer durationHours) {
        this.durationHours = durationHours;
    }

    public void setStatus(AccessRequestStatus status) {
        this.status = status;
    }

    public void setApprovedByManager(String approvedByManager) {
        this.approvedByManager = approvedByManager;
    }

    public void setApprovedByDevOps(String approvedByDevOps) {
        this.approvedByDevOps = approvedByDevOps;
    }

    public void setApprovedAtManager(LocalDateTime approvedAtManager) {
        this.approvedAtManager = approvedAtManager;
    }

    public void setApprovedAtDevOps(LocalDateTime approvedAtDevOps) {
        this.approvedAtDevOps = approvedAtDevOps;
    }
}
