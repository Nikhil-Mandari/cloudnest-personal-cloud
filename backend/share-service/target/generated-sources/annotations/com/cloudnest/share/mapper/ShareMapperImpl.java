package com.cloudnest.share.mapper;

import com.cloudnest.share.dto.ShareResponse;
import com.cloudnest.share.entity.Share;
import javax.annotation.processing.Generated;
import org.springframework.stereotype.Component;

@Generated(
    value = "org.mapstruct.ap.MappingProcessor",
    date = "2026-08-07T22:54:12+0530",
    comments = "version: 1.6.3, compiler: javac, environment: Java 25.0.2 (Oracle Corporation)"
)
@Component
public class ShareMapperImpl implements ShareMapper {

    @Override
    public ShareResponse toShareResponse(Share share) {
        if ( share == null ) {
            return null;
        }

        ShareResponse.ShareResponseBuilder shareResponse = ShareResponse.builder();

        shareResponse.resourceType( share.getResourceType() );
        shareResponse.permission( share.getPermission() );
        shareResponse.id( share.getId() );
        shareResponse.resourceId( share.getResourceId() );
        shareResponse.ownerId( share.getOwnerId() );
        shareResponse.sharedWithUserId( share.getSharedWithUserId() );
        shareResponse.shareToken( share.getShareToken() );
        shareResponse.isPublic( share.getIsPublic() );
        shareResponse.expiryDate( share.getExpiryDate() );
        shareResponse.viewCount( share.getViewCount() );
        shareResponse.downloadCount( share.getDownloadCount() );
        shareResponse.lastAccessedAt( share.getLastAccessedAt() );
        shareResponse.createdAt( share.getCreatedAt() );

        shareResponse.hasPassword( share.getPasswordHash() != null && !share.getPasswordHash().isBlank() );

        return shareResponse.build();
    }
}
