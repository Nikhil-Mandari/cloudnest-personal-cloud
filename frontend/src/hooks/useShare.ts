import { useMutation } from '@tanstack/react-query';
import { toast } from 'react-toastify';

import { shareService } from '@/services/share.service';
import type { CreateShareRequest } from '@/types';
import { getErrorMessage } from '@/utils/error';

/** Creates a share for a file (recipient + permission + optional expiry). */
export function useShareFileMutation() {
  return useMutation({
    mutationFn: (payload: CreateShareRequest) => shareService.shareFile(payload),
    onError: (error) => toast.error(getErrorMessage(error, 'Failed to create the share link.')),
  });
}
