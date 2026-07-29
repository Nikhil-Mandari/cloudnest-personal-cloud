package com.cloudnest.folder.mapper;

import com.cloudnest.folder.dto.FolderResponse;
import com.cloudnest.folder.entity.Folder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

/**
 * MapStruct mapper for converting between {@link Folder} entities and DTOs.
 */
@Mapper(componentModel = "spring")
public interface FolderMapper {

    /**
     * Converts a {@link Folder} entity into a {@link FolderResponse}.
     *
     * @param folder the folder entity (must not be null)
     * @return a populated {@link FolderResponse}
     */
    @Mapping(target = "deleted", source = "deleted")
    FolderResponse toFolderResponse(Folder folder);
}
