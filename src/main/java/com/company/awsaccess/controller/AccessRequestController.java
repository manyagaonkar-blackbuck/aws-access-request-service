package com.company.awsaccess.controller;

import com.company.awsaccess.dto.request.CreateAccessRequestDto;
import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/access-requests")
public class AccessRequestController {

    private final AccessRequestRepository repository;

    public AccessRequestController(AccessRequestRepository repository) {
        this.repository = repository;
    }

    @PostMapping
    public AccessRequestResponseDto createRequest(
            @RequestBody CreateAccessRequestDto dto
    ) {
        AccessRequest request = new AccessRequest();
        request.setRequesterEmail(dto.getRequesterEmail());
        request.setAwsAccount(dto.getAwsAccount());
        request.setReason(dto.getReason());
        request.setServices(dto.getServices());
        request.setResourceArns(dto.getResourceArns());

        AccessRequest saved = repository.save(request);

        return new AccessRequestResponseDto(
                saved.getId(),
                saved.getRequesterEmail(),
                saved.getAwsAccount(),
                saved.getStatus().name(),   // 🔥 FIX HERE
                saved.getCreatedAt()
        );
    }

    @GetMapping("/{id}")
    public AccessRequestResponseDto getRequest(@PathVariable Long id) {

        AccessRequest request = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Request not found"));

        return new AccessRequestResponseDto(
                request.getId(),
                request.getRequesterEmail(),
                request.getAwsAccount(),
                request.getStatus().name(), // 🔥 FIX HERE
                request.getCreatedAt()
        );
    }
}
