package com.company.awsaccess.controller;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;

    public AccessRequestController(AccessRequestService service) {
        this.service = service;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> body) {

        AccessRequest request = new AccessRequest();
        request.setRequesterEmail((String) body.get("requesterEmail"));
        request.setAwsAccount((String) body.get("awsAccount"));
        request.setReason((String) body.get("reason"));
        request.setServices((String) body.get("services"));
        request.setResourceArns((String) body.get("resourceArns"));
        request.setDurationHours((Integer) body.get("durationHours"));

        AccessRequest saved = service.createAccessRequest(request);

        return Map.of(
                "id", saved.getId(),
                "status", saved.getStatus().name(),
                "createdAt", saved.getCreatedAt()
        );
    }

    @PostMapping("/{id}/manager/approve")
    public AccessRequest managerApprove(@PathVariable Long id) {
        return service.approveByManager(id);
    }

    @PostMapping("/{id}/devops/approve")
    public AccessRequest devopsApprove(@PathVariable Long id) {
        return service.approveByDevOps(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> status(@PathVariable Long id) {
        return Map.of("status", service.getById(id).getStatus().name());
    }
}
