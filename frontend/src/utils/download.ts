import { fileService } from '@/services/file.service';
import type { FileItem } from '@/types';

/**
 * Downloads a file through the authenticated API and saves it to disk using
 * the original file name (object URL + programmatic anchor click).
 */
export async function downloadFileItem(file: FileItem): Promise<void> {
  const { data } = await fileService.downloadFile(file.id);
  const url = URL.createObjectURL(data);

  const anchor = document.createElement('a');
  anchor.href = url;
  anchor.download = file.originalFileName;
  document.body.appendChild(anchor);
  anchor.click();
  anchor.remove();

  // Revoke on the next tick so the download actually starts first.
  window.setTimeout(() => URL.revokeObjectURL(url), 1_000);
}

/** Copies text to the clipboard with a legacy fallback. Returns success. */
export async function copyToClipboard(text: string): Promise<boolean> {
  try {
    await navigator.clipboard.writeText(text);
    return true;
  } catch {
    try {
      const textarea = document.createElement('textarea');
      textarea.value = text;
      textarea.style.position = 'fixed';
      textarea.style.opacity = '0';
      document.body.appendChild(textarea);
      textarea.select();
      const ok = document.execCommand('copy');
      textarea.remove();
      return ok;
    } catch {
      return false;
    }
  }
}
