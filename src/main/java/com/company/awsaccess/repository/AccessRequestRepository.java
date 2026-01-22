package com.company.awsaccess.repository;

import com.company.awsaccess.model.AccessRequest;
import com.company.awsaccess.model.AccessRequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    List<AccessRequest> findByStatus(AccessRequestStatus status);
}
