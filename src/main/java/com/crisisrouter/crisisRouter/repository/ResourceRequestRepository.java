package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.UUID;
import org.springframework.data.repository.query.Param;

public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, UUID> {

    List<ResourceRequest> findByUserId(UUID creatorId);

    @Query(value = "SELECT * FROM resource_requests r " +
            "WHERE r.latitude IS NOT NULL AND r.longitude IS NOT NULL " +
            "AND (6371000 * acos(LEAST(1.0, cos(radians(:lat)) * cos(radians(r.latitude)) * " +
            "cos(radians(r.longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(r.latitude))))) <= :radius " +
            "ORDER BY (6371000 * acos(LEAST(1.0, cos(radians(:lat)) * cos(radians(r.latitude)) * " +
            "cos(radians(r.longitude) - radians(:lng)) + " +
            "sin(radians(:lat)) * sin(radians(r.latitude))))) ASC", nativeQuery = true)
    List<ResourceRequest> findNearbyRequests(
            @Param("lat") double latitude,
            @Param("lng") double longitude,
            @Param("radius") double radius);

    List<ResourceRequest> findByStatus(RequestStatus status);

}
