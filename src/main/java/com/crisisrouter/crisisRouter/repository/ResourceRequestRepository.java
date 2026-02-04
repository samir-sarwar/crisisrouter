package com.crisisrouter.crisisRouter.repository;

import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.List;
import java.util.UUID;
import org.locationtech.jts.geom.Point;
import org.springframework.data.repository.query.Param;


public interface ResourceRequestRepository extends JpaRepository<ResourceRequest, UUID> {

    List<ResourceRequest> findByUserId(UUID creatorId);

    @Query(value = "SELECT * FROM resource_requests r " +
            "WHERE ST_DWithin(r.location, :point, :radius) = true " +
            "ORDER BY ST_Distance(r.location, :point) ASC",
            nativeQuery = true)
    List<ResourceRequest> findNearbyRequests(@Param("point") Point point, @Param("radius") double radius);

    List<ResourceRequest> findByStatus(String status);


}
