package com.crisisrouter.crisisRouter.service.mapper;

import com.crisisrouter.crisisRouter.model.entity.ResourceRequest;
import com.crisisrouter.crisisRouter.model.entity.RequestStatus;
import com.crisisrouter.crisisRouter.service.dto.ResourceRequestDTO;
import org.locationtech.jts.geom.Point;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;

@Mapper(componentModel = "spring", imports = {RequestStatus.class})
public abstract class ResourceRequestMapper {

    // TO DTO: Navigation for the creator names and converting the Point back to Lat/Long
    @Mapping(source = "user.firstName", target = "creatorFirstName")
    @Mapping(source = "user.lastName", target = "creatorLastName")
    @Mapping(source = "category.id", target = "categoryId")
    @Mapping(source = "location", target = "longitude", qualifiedByName = "pointToLongitude")
    @Mapping(source = "location", target = "latitude", qualifiedByName = "pointToLatitude")
    public abstract ResourceRequestDTO toDTO(ResourceRequest entity);

    // TO ENTITY: Ignore complex fields we set manually in the Service
    @Mapping(target = "user", ignore = true)
    @Mapping(target = "category", ignore = true)
    @Mapping(target = "location", ignore = true)
    @Mapping(target = "status", expression = "java(dto.getStatus() != null ? RequestStatus.valueOf(dto.getStatus()) : RequestStatus.OPEN)")
    public abstract ResourceRequest toEntity(ResourceRequestDTO dto);

    // --- CUSTOM CONVERSION LOGIC ---

    @Named("pointToLongitude")
    protected Double pointToLongitude(Point location) {
        return location != null ? location.getX() : null;
    }

    @Named("pointToLatitude")
    protected Double pointToLatitude(Point location) {
        return location != null ? location.getY() : null;
    }
}