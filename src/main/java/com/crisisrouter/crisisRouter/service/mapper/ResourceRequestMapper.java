package com.crisisrouter.crisisRouter.service.mapper;
import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;
import org.springframework.stereotype.Component;

@Component
public class ResourceRequestMapper {

    private final GeometryFactory factory = new GeometryFactory(new PrecisionModel(), 4326);

    public ResourceRequest toEntity(ResourceRequestDTO dto) {
        if (dto == null) {
            return null;
        }
        ResourceRequest entity = new ResourceRequest();
        entity.setId(dto.getId());
        entity.setTitle(dto.getTitle());
        entity.setDescription(dto.getDescription());
        entity.setAddress(dto.getAddress());
        entity.setSeverityLevel(dto.getSeverityLevel());
        entity.setCustomCategory(dto.getCustomCategory());

        if(dto.getStatus() != null) {
            entity.setStatus(RequestStatus.valueOf(dto.getStatus()));
        }

        if (dto.getLongitude()!= null &&  dto.getLatitude()!= null) {
            Point location = factory.createPoint(new Coordinate(dto.getLongitude(), dto.getLatitude()));
            entity.setLocation(location);
        }

        return entity;
        }

    public ResourceRequestDTO toDTO(ResourceRequest entity) {
        if  (entity == null) {
            return null;
    }
        ResourceRequestDTO dto = new ResourceRequestDTO();
        dto.setId(entity.getId());
        dto.setTitle(entity.getTitle());
        dto.setDescription(entity.getDescription());
        dto.setAddress(entity.getAddress());
        dto.setSeverityLevel(entity.getSeverityLevel());
        dto.setCustomCategory(entity.getCustomCategory());
        dto.setStatus(entity.getStatus().name());

        if (dto.getCategoryId() != null) {
            dto.setCategoryId(dto.getCategoryId());
        }

        if (entity.getLocation() != null) {
            dto.setLongitude(entity.getLocation().getX());
            dto.setLatitude(entity.getLocation().getY());
        }

        return dto;
    }
}
