package com.company.awsaccess.controller;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.service.AccessRequestService;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestService service;
    private final AccessRequestRepository repository;

    public AccessRequestController(
            AccessRequestService service,
            AccessRequestRepository repository) {
        this.service = service;
        this.repository = repository;
    }

    @PostMapping
    public AccessRequest create(@RequestBody AccessRequest request) {
        return service.createAccessRequest(request);
    }

    @PostMapping("/{id}/manager/approve")
    public AccessRequest managerApprove(@PathVariable Long id) {
        return service.approveByManager(id);
    }

    @PostMapping("/{id}/manager/reject")
    public AccessRequest managerReject(@PathVariable Long id) {
        return service.rejectByManager(id);
    }

    @PostMapping("/{id}/devops/approve")
    public AccessRequest devopsApprove(@PathVariable Long id) {
        return service.approveByDevOps(id);
    }

    @PostMapping("/{id}/devops/reject")
    public AccessRequest devopsReject(@PathVariable Long id) {
        return service.rejectByDevOps(id);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> status(@PathVariable Long id) {
        return Map.of("status", service.getById(id).getStatus().name());
    }

    @GetMapping
    public List<AccessRequest> listAll() {
        return repository.findAll();
    }
}
