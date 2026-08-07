package com.cloudnest.file.mapper;

import com.cloudnest.file.dto.FileMetadataResponse;
import com.cloudnest.file.dto.FileResponse;
import com.cloudnest.file.dto.UpdateFileRequest;
import com.cloudnest.file.dto.UploadFileRequest;
import com.cloudnest.file.entity.FileMetadata;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-07T23:24:27+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class FileMapperImpl implements FileMapper {

    @Override
    public FileMetadata toEntity(UploadFileRequest request) {
        if ( request == null ) {
            return null;
        }

        FileMetadata.FileMetadataBuilder fileMetadata = FileMetadata.builder();

        fileMetadata.fileType( request.getContentType() );
        fileMetadata.originalFileName( request.getOriginalFileName() );
        fileMetadata.contentType( request.getContentType() );
        fileMetadata.fileSize( request.getFileSize() );
        fileMetadata.ownerId( request.getOwnerId() );
        fileMetadata.folderId( request.getFolderId() );
        fileMetadata.isPublic( request.getIsPublic() );
        fileMetadata.checksum( request.getChecksum() );

        return fileMetadata.build();
    }

    @Override
    public FileResponse toFileResponse(FileMetadata entity) {
        if ( entity == null ) {
            return null;
        }

        FileResponse.FileResponseBuilder fileResponse = FileResponse.builder();

        fileResponse.id( entity.getId() );
        fileResponse.fileId( entity.getFileId() );
        fileResponse.originalFileName( entity.getOriginalFileName() );
        fileResponse.objectName( entity.getObjectName() );
        fileResponse.bucketName( entity.getBucketName() );
        fileResponse.storedFileName( entity.getStoredFileName() );
        fileResponse.contentType( entity.getContentType() );
        fileResponse.fileType( entity.getFileType() );
        fileResponse.fileSize( entity.getFileSize() );
        fileResponse.storagePath( entity.getStoragePath() );
        fileResponse.ownerId( entity.getOwnerId() );
        fileResponse.folderId( entity.getFolderId() );
        fileResponse.isPublic( entity.getIsPublic() );
        fileResponse.isFavorite( entity.getIsFavorite() );
        fileResponse.checksum( entity.getChecksum() );
        fileResponse.uploadedAt( entity.getUploadedAt() );
        fileResponse.createdAt( entity.getCreatedAt() );
        fileResponse.updatedAt( entity.getUpdatedAt() );

        fileResponse.status( entity.getStatus() != null ? entity.getStatus().name() : "ACTIVE" );
        fileResponse.scanStatus( entity.getScanStatus() != null ? entity.getScanStatus().name() : "CLEAN" );

        return fileResponse.build();
    }

    @Override
    public FileMetadataResponse toMetadataResponse(FileMetadata entity) {
        if ( entity == null ) {
            return null;
        }

        FileMetadataResponse.FileMetadataResponseBuilder fileMetadataResponse = FileMetadataResponse.builder();

        fileMetadataResponse.id( entity.getId() );
        fileMetadataResponse.fileId( entity.getFileId() );
        fileMetadataResponse.originalFileName( entity.getOriginalFileName() );
        fileMetadataResponse.fileType( entity.getFileType() );
        fileMetadataResponse.fileSize( entity.getFileSize() );
        fileMetadataResponse.ownerId( entity.getOwnerId() );
        fileMetadataResponse.folderId( entity.getFolderId() );
        fileMetadataResponse.isPublic( entity.getIsPublic() );
        fileMetadataResponse.isFavorite( entity.getIsFavorite() );
        fileMetadataResponse.createdAt( entity.getCreatedAt() );
        fileMetadataResponse.updatedAt( entity.getUpdatedAt() );

        fileMetadataResponse.status( entity.getStatus() != null ? entity.getStatus().name() : "ACTIVE" );
        fileMetadataResponse.scanStatus( entity.getScanStatus() != null ? entity.getScanStatus().name() : "CLEAN" );

        return fileMetadataResponse.build();
    }

    @Override
    public void applyUpdate(FileMetadata entity, UpdateFileRequest request) {
        if ( request == null ) {
            return;
        }

        if ( request.getOriginalFileName() != null ) {
            entity.setOriginalFileName( request.getOriginalFileName() );
        }
        if ( request.getFileType() != null ) {
            entity.setFileType( request.getFileType() );
        }
        if ( request.getIsPublic() != null ) {
            entity.setIsPublic( request.getIsPublic() );
        }
    }
}
