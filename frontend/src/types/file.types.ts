/**
 * File types (file-service).
 *
 * Named `FileItem` to avoid clashing with the global DOM `File` type.
 * The shape mirrors the backend `FileMetadataResponse` DTO — the list/search
 * endpoints also expose the internal numeric `id`, which every mutation
 * endpoint (rename, move, delete, download, favorite, …) requires.
 */

export type FileStatus = 'ACTIVE' | 'DELETED';

/** Virus-scan lifecycle of a file's content. */
export type ScanStatus = 'PENDING' | 'SCANNING' | 'CLEAN' | 'INFECTED' | 'ERROR';

/** How the server should handle a duplicate-content upload. */
export type DuplicateAction = 'ASK' | 'KEEP_BOTH' | 'SKIP' | 'REPLACE';

/** An existing file with identical content (SHA-256), returned on duplicates. */
export interface DuplicateFileInfo {
  id: number;
  fileId: string;
  originalFileName: string;
  fileSize: number;
  checksum?: string;
  folderId?: string | null;
  uploadedAt?: string;
}

/** Upload response with duplicate detection. */
export interface UploadResult {
  duplicate: boolean;
  actionTaken: DuplicateAction;
  file?: FileDetail | null;
  duplicateOf?: DuplicateFileInfo | null;
}

export interface FileItem {
  /** Internal numeric primary key — used by all mutation endpoints. */
  id: number;
  /** Public-facing unique file identifier (UUID). */
  fileId: string;
  /** Original file name as uploaded (e.g. "report.pdf"). */
  originalFileName: string;
  /** MIME type / file type category (e.g. "application/pdf"). */
  fileType: string;
  /** File size in bytes. */
  fileSize: number;
  /** ID of the owning user. */
  ownerId: number;
  /** ID of the folder the file belongs to (null = root). */
  folderId: string | null;
  /** Whether the file is publicly accessible. */
  isPublic: boolean;
  /** Whether the owner marked the file as a favorite. */
  isFavorite: boolean;
  /** Lifecycle status. */
  status: FileStatus;
  /** Virus-scan status of the file's content. */
  scanStatus?: ScanStatus;
  /** Timestamp when the record was created (upload date). */
  createdAt: string;
  /** Timestamp when the record was last updated. */
  updatedAt: string;
}

/**
 * Detailed file response (backend `FileResponse` DTO) — returned by the
 * upload, rename, move and favorite endpoints. It carries extra storage
 * fields; normalize it into a `FileItem` with `mapFileResponseToItem`.
 */
export interface FileDetail {
  id: number;
  fileId: string;
  originalFileName: string;
  objectName: string;
  bucketName: string;
  storedFileName?: string;
  /** Canonical MIME content type. */
  contentType: string;
  /** Legacy alias of `contentType`. */
  fileType?: string;
  fileSize: number;
  storagePath?: string;
  ownerId: number;
  folderId: string | null;
  isPublic: boolean;
  isFavorite: boolean;
  checksum?: string;
  status: FileStatus;
  /** Virus-scan status of the file's content. */
  scanStatus?: ScanStatus;
  /** Timestamp when the file content was uploaded. */
  uploadedAt: string;
  createdAt: string;
  updatedAt: string;
}
