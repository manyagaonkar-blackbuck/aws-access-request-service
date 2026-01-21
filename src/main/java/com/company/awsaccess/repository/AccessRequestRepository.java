package com.company.awsaccess.repository;

import com.company.awsaccess.model.AccessRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {
}
