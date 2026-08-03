import { API_BASE_URL } from '@/constants/app';
import { API_ENDPOINTS } from '@/constants/apiEndpoints';
import type {
  FileDetail,
  FileItem,
  FileTypeCategory,
  FileTypeFilter,
  SortDirection,
  SortKey,
} from '@/types';

/**
 * File domain helpers: extension detection, type categorisation, formatting,
 * normalisation, sorting and filtering.
 */

/** Returns the lowercase extension of a file name (without the dot). */
export function getFileExtension(fileName: string): string {
  const dot = fileName.lastIndexOf('.');
  if (dot <= 0 || dot === fileName.length - 1) {
    return '';
  }
  return fileName.slice(dot + 1).toLowerCase();
}

/** Splits "report.final.pdf" into `{ base: "report.final", ext: "pdf" }`. */
export function splitFileName(fileName: string): { base: string; ext: string } {
  const dot = fileName.lastIndexOf('.');
  if (dot <= 0) {
    return { base: fileName, ext: '' };
  }
  return { base: fileName.slice(0, dot), ext: fileName.slice(dot + 1) };
}

const EXTENSION_CATEGORIES: Record<FileTypeCategory, readonly string[]> = {
  image: ['png', 'jpg', 'jpeg', 'gif', 'webp', 'svg', 'bmp', 'ico', 'heic', 'avif', 'tiff'],
  video: ['mp4', 'mov', 'avi', 'mkv', 'webm', 'wmv', 'flv', 'm4v', '3gp'],
  audio: ['mp3', 'wav', 'ogg', 'aac', 'flac', 'm4a', 'wma', 'opus'],
  document: [
    'pdf',
    'doc',
    'docx',
    'xls',
    'xlsx',
    'ppt',
    'pptx',
    'txt',
    'md',
    'rtf',
    'csv',
    'odt',
    'ods',
    'odp',
  ],
  archive: ['zip', 'rar', '7z', 'tar', 'gz', 'bz2', 'xz', 'tgz'],
  code: [
    'js',
    'ts',
    'tsx',
    'jsx',
    'py',
    'java',
    'c',
    'cpp',
    'cs',
    'go',
    'rs',
    'rb',
    'php',
    'html',
    'css',
    'scss',
    'json',
    'xml',
    'yml',
    'yaml',
    'sh',
    'sql',
    'kt',
    'swift',
  ],
  other: [],
};

/** Classifies a file into a coarse category, preferring the MIME type. */
export function getFileTypeCategory(
  file: Pick<FileItem, 'fileType' | 'originalFileName'>,
): FileTypeCategory {
  const mime = (file.fileType ?? '').toLowerCase();

  if (mime.startsWith('image/')) return 'image';
  if (mime.startsWith('video/')) return 'video';
  if (mime.startsWith('audio/')) return 'audio';
  if (
    mime.includes('pdf') ||
    mime.includes('text/') ||
    mime.includes('document') ||
    mime.includes('spreadsheet') ||
    mime.includes('presentation') ||
    mime.includes('msword')
  ) {
    return 'document';
  }
  if (
    mime.includes('zip') ||
    mime.includes('compressed') ||
    mime.includes('archive') ||
    mime.includes('gzip') ||
    mime.includes('x-7z')
  ) {
    return 'archive';
  }

  const ext = getFileExtension(file.originalFileName);
  const entries = Object.entries(EXTENSION_CATEGORIES) as Array<
    [FileTypeCategory, readonly string[]]
  >;
  const match = entries.find(([, extensions]) => extensions.includes(ext));
  return match?.[0] ?? 'other';
}

/** Formats an ISO timestamp as a friendly, human-relative date. */
export function formatFileDate(iso: string): string {
  const date = new Date(iso);
  if (Number.isNaN(date.getTime())) {
    return '—';
  }

  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate()).getTime();
  const startOfFile = new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime();
  const daysAgo = Math.round((startOfToday - startOfFile) / 86_400_000);

  if (daysAgo <= 0) return 'Today';
  if (daysAgo === 1) return 'Yesterday';
  if (daysAgo < 7) return `${daysAgo} days ago`;

  const options: Intl.DateTimeFormatOptions = { month: 'short', day: 'numeric' };
  if (date.getFullYear() !== now.getFullYear()) {
    options.year = 'numeric';
  }
  return new Intl.DateTimeFormat(undefined, options).format(date);
}

/** Stable, deterministic sort used by the explorer (never mutates input). */
export function sortFiles(files: FileItem[], key: SortKey, direction: SortDirection): FileItem[] {
  const dir = direction === 'asc' ? 1 : -1;

  return [...files].sort((a, b) => {
    switch (key) {
      case 'name':
        return (
          a.originalFileName.localeCompare(b.originalFileName, undefined, {
            numeric: true,
            sensitivity: 'base',
          }) * dir
        );
      case 'size':
        return (a.fileSize - b.fileSize) * dir;
      case 'date':
        return (new Date(a.createdAt).getTime() - new Date(b.createdAt).getTime()) * dir;
      case 'type':
        return getFileTypeCategory(a).localeCompare(getFileTypeCategory(b)) * dir;
      default:
        return 0;
    }
  });
}

/** Applies the search query and type/favorites filter. */
export function filterFiles(
  files: FileItem[],
  searchQuery: string,
  filter: FileTypeFilter,
): FileItem[] {
  const query = searchQuery.trim().toLowerCase();

  return files.filter((file) => {
    if (query && !file.originalFileName.toLowerCase().includes(query)) {
      return false;
    }
    if (filter === 'all') {
      return true;
    }
    if (filter === 'favorites') {
      return file.isFavorite;
    }
    return getFileTypeCategory(file) === filter;
  });
}

/**
 * Normalises a detailed file response (upload / rename / move / favorite) into
 * the lighter `FileItem` shape used by listings.
 */
export function mapFileResponseToItem(response: FileDetail): FileItem {
  return {
    id: response.id,
    fileId: response.fileId,
    originalFileName: response.originalFileName,
    fileType: response.fileType ?? response.contentType,
    fileSize: response.fileSize,
    ownerId: response.ownerId,
    folderId: response.folderId ?? null,
    isPublic: response.isPublic,
    isFavorite: response.isFavorite ?? false,
    status: response.status,
    createdAt: response.createdAt ?? response.uploadedAt,
    updatedAt: response.updatedAt ?? response.uploadedAt,
  };
}

/** Public, unauthenticated access URL for a share token. */
export function buildShareUrl(token: string): string {
  return `${API_BASE_URL}${API_ENDPOINTS.share.public(token)}`;
}
