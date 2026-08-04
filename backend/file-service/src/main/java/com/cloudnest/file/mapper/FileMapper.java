package com.cloudnest.file.mapper;

import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.entity.FileMetadata;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

/**
 * MapStruct mapper for converting between {@link FileMetadata} entities and DTOs.
 * <p>
 * Handles mapping logic for uploading, updating, and responding with file metadata.
 */
@Mapper(
        componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE
)
public interface FileMapper {

    /**
     * Converts an upload request into a new {@link FileMetadata} entity.
     * <p>
     * Storage-derived fields ({@code objectName}, {@code bucketName},
     * {@code storagePath}, {@code uploadedAt}, {@code fileId},
     * {@code storedFileName}) are set by the service after upload.
     *
     * @param request the upload request
     * @return a new FileMetadata entity with fields populated from the request
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "storedFileName", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    @Mapping(target = "bucketName", ignore = true)
    @Mapping(target = "storagePath", ignore = true)
    @Mapping(target = "fileType", source = "contentType")
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    FileMetadata toEntity(UploadFileRequest request);

    /**
     * Converts a {@link FileMetadata} entity into a detailed {@link FileResponse}.
     *
     * @param entity the file metadata entity
     * @return a populated detailed response
     */
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    FileResponse toFileResponse(FileMetadata entity);

    /**
     * Converts a {@link FileMetadata} entity into a lightweight {@link FileMetadataResponse}.
     *
     * @param entity the file metadata entity
     * @return a populated lightweight response
     */
    @Mapping(target = "status", expression = "java(entity.getStatus().name())")
    FileMetadataResponse toMetadataResponse(FileMetadata entity);

    /**
     * Applies non-null fields from an {@link UpdateFileRequest} to an existing
     * {@link FileMetadata} entity (partial update).
     * <p>
     * Note: This is a generated MapStruct implementation — use it as a blueprint
     * or replace with manual mapping if MapStruct is not configured.
     *
     * @param entity  the target entity to update (mutated in place)
     * @param request the update payload with optional fields
     */
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "fileId", ignore = true)
    @Mapping(target = "storedFileName", ignore = true)
    @Mapping(target = "objectName", ignore = true)
    @Mapping(target = "bucketName", ignore = true)
    @Mapping(target = "contentType", ignore = true)
    @Mapping(target = "fileSize", ignore = true)
    @Mapping(target = "storagePath", ignore = true)
    @Mapping(target = "ownerId", ignore = true)
    @Mapping(target = "folderId", ignore = true)
    @Mapping(target = "checksum", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    @Mapping(target = "updatedAt", ignore = true)
    void applyUpdate(@MappingTarget FileMetadata entity, UpdateFileRequest request);
}
