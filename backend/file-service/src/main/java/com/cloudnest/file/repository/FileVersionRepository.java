package com.cloudnest.file.repository;

import com.cloudnest.file.entity.FileVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for {@link FileVersion} entity operations.
 */
@Repository
public interface FileVersionRepository extends JpaRepository<FileVersion, Long> {

    /**
     * Lists all archived versions of a file, newest first.
     */
    List<FileVersion> findByFileMetadataIdOrderByVersionNumberDesc(Long fileMetadataId);

    /**
     * Finds a specific version of a specific file.
     */
    Optional<FileVersion> findByIdAndFileMetadataId(Long id, Long fileMetadataId);

    /**
     * Highest version number archived for a file (empty when no versions yet).
     */
    @Query("SELECT MAX(v.versionNumber) FROM FileVersion v WHERE v.fileMetadataId = :fileMetadataId")
    Optional<Integer> findMaxVersionNumberByFileMetadataId(@Param("fileMetadataId") Long fileMetadataId);

    /**
     * Whether a file has any archived versions.
     */
    boolean existsByFileMetadataId(Long fileMetadataId);

    /**
     * Whether another version row references the same MinIO object.
     * <p>
     * A restore-then-replace flow can leave two version rows pointing at the
     * same object; deleting one must never destroy content the other references.
     */
    boolean existsByObjectNameAndIdNot(String objectName, Long id);
}
