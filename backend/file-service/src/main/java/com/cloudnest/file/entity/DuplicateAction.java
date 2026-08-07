package com.cloudnest.file.entity;

/**
 * How an upload should behave when a duplicate (identical SHA-256 checksum)
 * already exists for the same owner.
 *
 * <ul>
 *   <li>{@code ASK} — detect duplicates and report them without uploading
 *       (the client then decides and re-uploads with a concrete action)</li>
 *   <li>{@code KEEP_BOTH} — always create a new file record, even when a
 *       duplicate exists</li>
 *   <li>{@code SKIP} — do not upload when a duplicate exists</li>
 *   <li>{@code REPLACE} — upload the new content as a new version of the
 *       existing duplicate file (the previous content is archived)</li>
 * </ul>
 */
public enum DuplicateAction {
    ASK,
    KEEP_BOTH,
    SKIP,
    REPLACE
}
