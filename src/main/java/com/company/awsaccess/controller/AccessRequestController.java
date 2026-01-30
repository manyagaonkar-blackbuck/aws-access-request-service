package com.company.awsaccess.controller;

import com.company.awsaccess.dto.ApiResponse;
import com.company.awsaccess.dto.mapper.AccessRequestMapper;
import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.repository.AccessRequestRepository;
import com.company.awsaccess.service.AccessRequestService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

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

    // ✅ CREATE REQUEST (DTO-BASED, CLEAN)
    @PostMapping
    public ApiResponse<AccessRequest> create(
            @RequestBody CreateAccessRequestDto dto
    ) {
        return ApiResponse.success(service.create(dto));
    }

    // ✅ DASHBOARD API (NO UI CHANGE)
    @GetMapping
    public ApiResponse<List<AccessRequestResponseDto>> getAll() {

        List<AccessRequestResponseDto> response =
                repository.findAll()
                        .stream()
                        .map(AccessRequestMapper::toDto)
                        .collect(Collectors.toList());

        return ApiResponse.success(response);
    }

    // STATUS
    @GetMapping("/{id}/status")
    public ApiResponse<String> status(@PathVariable Long id) {
        return ApiResponse.success(service.getById(id).getStatus().name());
    }

    // MANAGER
    @PostMapping("/{id}/manager/approve")
    public ApiResponse<AccessRequest> managerApprove(@PathVariable Long id) {
        return ApiResponse.success(service.approveByManager(id));
    }

    @PostMapping("/{id}/manager/reject")
    public ApiResponse<AccessRequest> managerReject(@PathVariable Long id) {
        return ApiResponse.success(service.rejectByManager(id));
    }

    // DEVOPS
    @PostMapping("/{id}/devops/approve")
    public ApiResponse<AccessRequest> devopsApprove(@PathVariable Long id) {
        return ApiResponse.success(service.approveByDevOps(id));
    }

    @PostMapping("/{id}/devops/reject")
    public ApiResponse<AccessRequest> devopsReject(@PathVariable Long id) {
        return ApiResponse.success(service.rejectByDevOps(id));
    }
}
