package com.cloudnest.share.mapper;

import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.entity.Share;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link Share} entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface ShareMapper {

    /**
     * Converts a {@link Share} entity into a {@link ShareResponse}.
     *
     * @param share the share entity (must not be null)
     * @return a populated {@link ShareResponse}
     */
    @Mapping(target = "resourceType", source = "resourceType")
    @Mapping(target = "permission", source = "permission")
    ShareResponse toShareResponse(Share share);
}
