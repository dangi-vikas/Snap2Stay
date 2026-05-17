import { motion } from 'framer-motion';
import { ChevronRight, MapPin, Sparkles } from 'lucide-react';
import type { PropertyMatch } from '@/types/api';
import { ScoreBar } from './ScoreBar';
import { cn } from '@/lib/cn';

interface MatchCardProps {
  match: PropertyMatch;
  index: number;
  primary?: boolean;
}

/**
 * The "primary match" card. Larger image, score bar, AI explanation snippet.
 * Stagger-fades in with a small upward motion for delight.
 */
export function MatchCard({ match, index, primary }: MatchCardProps) {
  return (
    <motion.article
      initial={{ opacity: 0, y: 32 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 + index * 0.12, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      whileHover={{ y: -4 }}
      className={cn(
        'group bg-white rounded-xl overflow-hidden border border-bonvoy-line',
        'shadow-card hover:shadow-card-hover transition-shadow duration-300',
        'flex flex-col md:flex-row',
        primary && index === 0 && 'ring-2 ring-snap-glow/40 ring-offset-2 ring-offset-bonvoy-surface',
      )}
    >
      <div className="relative md:w-2/5 aspect-[4/3] md:aspect-auto overflow-hidden bg-bonvoy-line">
        <motion.img
          src={match.thumbnailUrl}
          alt={match.name}
          className="w-full h-full object-cover"
          whileHover={{ scale: 1.05 }}
          transition={{ duration: 0.6 }}
        />
        {primary && index === 0 && (
          <div className="absolute top-3 left-3 flex items-center gap-1.5 bg-bonvoy-ink text-white text-xs font-bold px-3 py-1.5 rounded-full">
            <Sparkles className="w-3 h-3" />
            BEST MATCH
          </div>
        )}
        {!match.available && (
          <div className="absolute top-3 right-3 bg-white/95 backdrop-blur-sm text-bonvoy-charcoal text-[10px] font-bold uppercase tracking-wider px-2.5 py-1 rounded-full">
            Sold Out
          </div>
        )}
        {match.brand && (
          <div className="absolute bottom-3 left-3 bg-white/95 backdrop-blur-sm text-bonvoy-ink text-xs font-semibold px-3 py-1 rounded-full">
            {match.brand}
          </div>
        )}
      </div>

      <div className="flex-1 p-6 flex flex-col gap-3">
        <div className="flex items-start justify-between gap-3">
          <h3 className="text-xl font-bold text-bonvoy-ink leading-tight">
            {match.name}
          </h3>
        </div>

        {match.city && (
          <div className="flex items-center gap-1.5 text-sm text-bonvoy-slate">
            <MapPin className="w-3.5 h-3.5" />
            {match.city}
            {match.marketCode && (
              <span className="text-bonvoy-mist">· {match.marketCode}</span>
            )}
          </div>
        )}

        {primary && (
          <div>
            <div className="flex items-center justify-between mb-1.5">
              <span className="text-[11px] font-semibold uppercase tracking-wider text-bonvoy-slate">
                Visual Match
              </span>
            </div>
            <ScoreBar score={match.matchScore} delay={0.3 + index * 0.12} />
          </div>
        )}

        {match.explanation && (
          <p className="text-sm text-bonvoy-charcoal italic">
            "{match.explanation}"
          </p>
        )}

        <div className="mt-auto pt-3 flex items-center justify-between border-t border-bonvoy-line">
          <button className="text-sm font-semibold text-bonvoy-ink hover:text-snap-glow transition-colors flex items-center gap-1 group/btn">
            View Details
            <ChevronRight className="w-4 h-4 transition-transform group-hover/btn:translate-x-0.5" />
          </button>
          <button
            className={cn(
              'btn-primary text-xs px-5 py-2',
              !match.available && 'opacity-40 cursor-not-allowed',
            )}
            disabled={!match.available}
          >
            {match.available ? 'View Rates' : 'Unavailable'}
          </button>
        </div>
      </div>
    </motion.article>
  );
}
