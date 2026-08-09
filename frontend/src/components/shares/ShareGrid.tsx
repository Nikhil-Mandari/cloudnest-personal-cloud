import { AnimatePresence, motion } from 'framer-motion';

import type { ShareRecord } from '@/types';
import { ShareCard } from './ShareCard';

export interface ShareGridProps {
  shares: ShareRecord[];
  onCopyLink: (share: ShareRecord) => void;
}

/** Responsive animated grid of shared item cards. */
export function ShareGrid({ shares, onCopyLink }: ShareGridProps) {
  return (
    <motion.div
      layout
      className="grid grid-cols-2 gap-3 sm:grid-cols-3 md:gap-4 lg:grid-cols-4 xl:grid-cols-5 2xl:grid-cols-6"
    >
      <AnimatePresence mode="popLayout">
        {shares.map((share) => (
          <ShareCard key={share.id} share={share} onCopyLink={onCopyLink} />
        ))}
      </AnimatePresence>
    </motion.div>
  );
}
