package com.company.awsaccess.dto.mapper;

import com.company.awsaccess.dto.response.AccessRequestResponseDto;
import com.company.awsaccess.model.AccessRequest;

public class AccessRequestMapper {

    public static AccessRequestResponseDto toDto(AccessRequest req) {
        return new AccessRequestResponseDto(
                req.getId(),
                req.getRequesterEmail(),
                req.getAwsAccount(),
                req.getStatus().name(),
                req.getCreatedAt()
        );
    }
}
