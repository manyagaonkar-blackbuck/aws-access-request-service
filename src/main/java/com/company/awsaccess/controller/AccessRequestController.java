package com.company.awsaccess.controller;

import com.company.awsaccess.dto.ApiResponse;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.repository.AccessRequestRepository;
import com.company.awsaccess.service.AccessRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
@CrossOrigin(origins = "http://localhost:3000")
public class AccessRequestController {

    private final AccessRequestService service;
    private final AccessRequestRepository repository;

    public AccessRequestController(
            AccessRequestService service,
            AccessRequestRepository repository
    ) {
        this.service = service;
        this.repository = repository;
    }

    // CREATE REQUEST
    @PostMapping
    public ApiResponse<AccessRequest> create(@RequestBody Map<String, Object> body) {

        AccessRequest request = new AccessRequest();
        request.setRequesterEmail((String) body.get("requesterEmail"));
        request.setAwsAccount((String) body.get("awsAccount"));
        request.setReason((String) body.get("reason"));
        request.setServices((String) body.get("services"));
        request.setResourceArns((String) body.get("resourceArns"));
        request.setDurationHours((Integer) body.get("durationHours"));

        return ApiResponse.success(service.createAccessRequest(request));
    }

    // ✅ GET ALL REQUESTS (DASHBOARD)
    @GetMapping
    public ApiResponse<List<AccessRequest>> getAll() {
        return ApiResponse.success(repository.findAll());
    }

    @PostMapping("/{id}/manager/approve")
    public ApiResponse<AccessRequest> managerApprove(@PathVariable Long id) {
        return ApiResponse.success(service.approveByManager(id));
    }

    @PostMapping("/{id}/manager/reject")
    public ApiResponse<AccessRequest> managerReject(@PathVariable Long id) {
        return ApiResponse.success(service.rejectByManager(id));
    }

    @PostMapping("/{id}/devops/approve")
    public ApiResponse<AccessRequest> devopsApprove(@PathVariable Long id) {
        return ApiResponse.success(service.approveByDevOps(id));
    }

    @PostMapping("/{id}/devops/reject")
    public ApiResponse<AccessRequest> devopsReject(@PathVariable Long id) {
        return ApiResponse.success(service.rejectByDevOps(id));
    }

    @GetMapping("/{id}/status")
    public ApiResponse<String> status(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id).getStatus().name());
    }
}
