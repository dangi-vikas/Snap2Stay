import { motion } from 'framer-motion';
import { ChevronRight } from 'lucide-react';
import type { PropertyMatch } from '@/types/api';

interface NearbyCardProps {
  property: PropertyMatch;
  index: number;
}

/**
 * Compact card for the "Other properties in <city>" rail.
 * Smaller than the primary match card; shows the property without a score
 * (these are not matched on the photo, only on location).
 */
export function NearbyCard({ property, index }: NearbyCardProps) {
  return (
    <motion.article
      initial={{ opacity: 0, x: 24 }}
      animate={{ opacity: 1, x: 0 }}
      transition={{ delay: 0.4 + index * 0.08, duration: 0.5 }}
      whileHover={{ y: -4 }}
      className="group bg-white rounded-xl overflow-hidden border border-bonvoy-line
                 shadow-card hover:shadow-card-hover transition-shadow duration-300
                 flex flex-col w-[280px] flex-shrink-0"
    >
      <div className="relative aspect-[4/3] overflow-hidden bg-bonvoy-line">
        <motion.img
          src={property.thumbnailUrl}
          alt={property.name}
          className="w-full h-full object-cover"
          whileHover={{ scale: 1.06 }}
          transition={{ duration: 0.6 }}
        />
        {property.brand && (
          <div className="absolute bottom-2 left-2 bg-white/95 backdrop-blur-sm text-bonvoy-ink text-[10px] font-semibold px-2 py-0.5 rounded-full">
            {property.brand}
          </div>
        )}
      </div>
      <div className="p-4 flex flex-col gap-2 flex-1">
        <h4 className="text-sm font-bold text-bonvoy-ink leading-tight line-clamp-2">
          {property.name}
        </h4>
        {property.city && (
          <p className="text-xs text-bonvoy-slate">{property.city}</p>
        )}
        <button className="mt-auto pt-2 text-xs font-semibold text-bonvoy-ink hover:text-snap-glow transition-colors flex items-center gap-1 group/btn">
          View
          <ChevronRight className="w-3.5 h-3.5 transition-transform group-hover/btn:translate-x-0.5" />
        </button>
      </div>
    </motion.article>
  );
}
