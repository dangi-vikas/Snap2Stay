import { motion } from 'framer-motion';

interface ScoreBarProps {
  score: number; // 0..1
  delay?: number;
}

export function ScoreBar({ score, delay = 0 }: ScoreBarProps) {
  const pct = Math.round(score * 100);
  const color =
    score >= 0.85 ? 'from-emerald-500 to-emerald-600' :
    score >= 0.7  ? 'from-snap-glow to-bonvoy-gold' :
                    'from-amber-400 to-amber-500';
  return (
    <div className="flex items-center gap-3 w-full">
      <div className="flex-1 h-1.5 bg-bonvoy-line rounded-full overflow-hidden">
        <motion.div
          initial={{ width: 0 }}
          animate={{ width: `${pct}%` }}
          transition={{ delay, duration: 0.9, ease: [0.22, 1, 0.36, 1] }}
          className={`h-full bg-gradient-to-r ${color} rounded-full`}
        />
      </div>
      <span className="text-xs font-bold text-bonvoy-ink tabular-nums w-10 text-right">
        {pct}%
      </span>
    </div>
  );
}
