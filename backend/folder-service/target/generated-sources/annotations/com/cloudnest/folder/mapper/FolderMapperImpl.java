package com.cloudnest.folder.mapper;

import com.cloudnest.folder.dto.FolderResponse;
import com.cloudnest.folder.entity.Folder;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-04T23:17:43+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class FolderMapperImpl implements FolderMapper {

    @Override
    public FolderResponse toFolderResponse(Folder folder) {
        if ( folder == null ) {
            return null;
        }

        FolderResponse.FolderResponseBuilder folderResponse = FolderResponse.builder();

        folderResponse.deleted( folder.getDeleted() );
        folderResponse.id( folder.getId() );
        folderResponse.name( folder.getName() );
        folderResponse.ownerId( folder.getOwnerId() );
        folderResponse.parentFolderId( folder.getParentFolderId() );
        folderResponse.path( folder.getPath() );
        folderResponse.level( folder.getLevel() );
        folderResponse.createdAt( folder.getCreatedAt() );
        folderResponse.updatedAt( folder.getUpdatedAt() );

        return folderResponse.build();
    }
}
