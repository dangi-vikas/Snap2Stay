import { Camera } from 'lucide-react';
import { motion } from 'framer-motion';
import { cn } from '@/lib/cn';

interface CameraFabProps {
  onClick: () => void;
  /** Hide the radiating rings while the modal is open to avoid noise */
  pulsing?: boolean;
  className?: string;
}

/**
 * Snap2Stay camera entry point. Floats bottom-right with two staggered ripple
 * rings that radiate outward continuously, drawing the eye without being
 * obnoxious. Hover lifts the button and softens the glow ramp.
 */
export function CameraFab({ onClick, pulsing = true, className }: CameraFabProps) {
  return (
    <motion.button
      onClick={onClick}
      aria-label="Search by photo"
      initial={{ scale: 0, opacity: 0 }}
      animate={{ scale: 1, opacity: 1 }}
      transition={{ delay: 0.9, duration: 0.5, type: 'spring', stiffness: 280, damping: 18 }}
      whileHover={{ scale: 1.06 }}
      whileTap={{ scale: 0.94 }}
      className={cn(
        'fixed bottom-8 right-8 z-40',
        'w-16 h-16 rounded-full',
        'bg-gradient-to-br from-snap-glow to-bonvoy-gold',
        'flex items-center justify-center',
        'text-white shadow-fab hover:shadow-fab-hover',
        'transition-shadow duration-300',
        'group',
        className,
      )}
    >
      {pulsing && (
        <>
          <span
            aria-hidden
            className="absolute inset-0 rounded-full bg-snap-glow/40 animate-pulse-ring"
          />
          <span
            aria-hidden
            className="absolute inset-0 rounded-full bg-snap-glow/40 animate-pulse-ring-delay"
          />
        </>
      )}

      <Camera className="w-7 h-7 relative z-10 drop-shadow-sm" strokeWidth={2.2} />

      {/* Tooltip on hover */}
      <span
        className="absolute right-full mr-3 top-1/2 -translate-y-1/2
                   whitespace-nowrap bg-bonvoy-ink text-white text-xs
                   px-3 py-1.5 rounded-md font-medium
                   opacity-0 group-hover:opacity-100
                   translate-x-2 group-hover:translate-x-0
                   transition-all duration-200 pointer-events-none"
      >
        Search with a photo
        <span
          className="absolute left-full top-1/2 -translate-y-1/2
                     border-4 border-transparent border-l-bonvoy-ink"
        />
      </span>
    </motion.button>
  );
}
