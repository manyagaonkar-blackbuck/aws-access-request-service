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

    @PrePersist
    public void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.durationHours != null) {
            this.expiresAt = this.createdAt.plusHours(this.durationHours);
        }
        if (this.status == null) {
            this.status = AccessRequestStatus.CREATED;
        }
    }

    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }

    // getters & setters

    public Long getId() { return id; }
    public String getRequesterEmail() { return requesterEmail; }
    public void setRequesterEmail(String requesterEmail) { this.requesterEmail = requesterEmail; }
    public String getAwsAccount() { return awsAccount; }
    public void setAwsAccount(String awsAccount) { this.awsAccount = awsAccount; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
    public String getServices() { return services; }
    public void setServices(String services) { this.services = services; }
    public String getResourceArns() { return resourceArns; }
    public void setResourceArns(String resourceArns) { this.resourceArns = resourceArns; }
    public Integer getDurationHours() { return durationHours; }
    public void setDurationHours(Integer durationHours) { this.durationHours = durationHours; }
    public AccessRequestStatus getStatus() { return status; }
    public void setStatus(AccessRequestStatus status) { this.status = status; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getExpiresAt() { return expiresAt; }
}
