package com.company.awsaccess.service;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import com.company.awsaccess.repository.AccessRequestRepository;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ExpiryScheduler {

    private final AccessRequestRepository repository;

    public ExpiryScheduler(AccessRequestRepository repository) {
        this.repository = repository;
    }

    @Scheduled(fixedRate = 600000) // every 10 mins
    public void expireRequests() {

        List<AccessRequest> requests =
                repository.findByStatus(AccessRequestStatus.DEVOPS_APPROVED);

        for (AccessRequest request : requests) {
            if (request.isExpired()) {
                request.setStatus(AccessRequestStatus.EXPIRED);
                repository.save(request);
            }
        }
    }
}
