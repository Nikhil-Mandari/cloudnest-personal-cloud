package com.cloudnest.file.repository;

import com.cloudnest.file.entity.FileMetadata;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link FileMetadata} entity operations.
 * <p>
 * Provides standard CRUD plus custom queries for file management features
 * such as searching, filtering by owner/folder, and checking uniqueness.
 */
@Repository
public interface FileMetadataRepository extends JpaRepository<FileMetadata, Long> {

    /**
     * Finds all file metadata records belonging to a specific owner.
     *
     * @param ownerId the ID of the file owner
     * @return a list of file metadata records owned by the specified user
     */
    List<FileMetadata> findByOwnerId(Long ownerId);

    /**
     * Finds a file metadata record by its public-facing file ID (UUID).
     *
     * @param fileId the unique file identifier
     * @return an {@link Optional} containing the matching record, or empty if not found
     */
    Optional<FileMetadata> findByFileId(String fileId);

    /**
     * Finds all file metadata records within a specific folder.
     *
     * @param folderId the ID of the folder
     * @return a list of file metadata records in the specified folder
     */
    List<FileMetadata> findByFolderId(Long folderId);

    /**
     * Checks whether a stored file name is already taken.
     *
     * @param storedFileName the stored file name to check
     * @return {@code true} if a record with the given stored file name exists
     */
    boolean existsByStoredFileName(String storedFileName);

    /**
     * Searches for active file metadata records by original file name (case-insensitive).
     * Only returns files with {@code ACTIVE} status.
     *
     * @param query   the search term
     * @param ownerId the ID of the file owner
     * @return a list of matching file metadata records
     */
    @Query("SELECT f FROM FileMetadata f WHERE " +
           "LOWER(f.originalFileName) LIKE LOWER(CONCAT('%', :query, '%')) " +
           "AND f.ownerId = :ownerId " +
           "AND f.status = 'ACTIVE'")
    List<FileMetadata> searchByFileName(@Param("query") String query, @Param("ownerId") Long ownerId);
}
