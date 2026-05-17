import { useNavigate } from 'react-router-dom';
import { useEffect } from 'react';
import { motion } from 'framer-motion';
import { ArrowLeft, Sparkles } from 'lucide-react';
import { BonvoyHeader } from '@/components/layout/BonvoyHeader';
import { BonvoyFooter } from '@/components/layout/BonvoyFooter';
import { MatchCard } from '@/components/results/MatchCard';
import { NearbyCard } from '@/components/results/NearbyCard';
import { useSearch } from '@/context/SearchContext';

export function ResultsPage() {
  const { result, uploadedPreview, reset } = useSearch();
  const navigate = useNavigate();

  // If user lands here without a result (e.g. refresh), bounce home.
  useEffect(() => {
    if (!result) {
      navigate('/', { replace: true });
    }
  }, [result, navigate]);

  if (!result) return null;

  const handleBack = () => {
    reset();
    navigate('/');
  };

  return (
    <div className="min-h-screen flex flex-col bg-bonvoy-surface">
      <BonvoyHeader variant="solid" />

      <main className="flex-1 max-w-[1280px] w-full mx-auto px-6 py-8">
        <button
          onClick={handleBack}
          className="text-sm text-bonvoy-slate hover:text-bonvoy-ink transition-colors flex items-center gap-1.5 mb-6 group"
        >
          <ArrowLeft className="w-4 h-4 transition-transform group-hover:-translate-x-0.5" />
          Search again
        </button>

        {/* Header strip: query photo + AI tags + summary */}
        <motion.section
          initial={{ opacity: 0, y: 16 }}
          animate={{ opacity: 1, y: 0 }}
          transition={{ duration: 0.5 }}
          className="bg-white rounded-2xl shadow-card border border-bonvoy-line overflow-hidden mb-10"
        >
          <div className="grid grid-cols-1 sm:grid-cols-[200px_1fr] gap-0">
            <div className="relative aspect-[4/3] sm:aspect-auto sm:h-full bg-bonvoy-line">
              {uploadedPreview && (
                <img
                  src={uploadedPreview}
                  alt="Your search"
                  className="w-full h-full object-cover"
                />
              )}
              <div className="absolute top-2 left-2 bg-white/95 backdrop-blur-sm text-[10px] font-bold uppercase tracking-wider text-bonvoy-ink px-2 py-1 rounded-full">
                Your photo
              </div>
            </div>
            <div className="p-6 flex flex-col gap-3">
              <div className="flex items-center gap-2">
                <Sparkles className="w-4 h-4 text-snap-glow" />
                <span className="text-[11px] font-bold uppercase tracking-[0.2em] text-snap-glow">
                  AI Visual Match
                </span>
              </div>
              <h1 className="text-2xl md:text-3xl font-bold text-bonvoy-ink leading-tight">
                We found {result.primaryMatches.length} {result.primaryMatches.length === 1 ? 'property' : 'properties'} matching your photo
              </h1>
              {result.debug?.caption && (
                <p className="text-sm text-bonvoy-slate italic">
                  Detected: "{result.debug.caption}"
                </p>
              )}
              {result.debug?.tags && result.debug.tags.length > 0 && (
                <div className="flex flex-wrap gap-2 pt-1">
                  {result.debug.tags.map((tag) => (
                    <span
                      key={tag}
                      className="tag-chip border-snap-glow/30 bg-snap-glow/5 text-bonvoy-ink"
                    >
                      {tag}
                    </span>
                  ))}
                </div>
              )}
              <div className="text-xs text-bonvoy-mist mt-auto pt-2">
                Search took {result.tookMs} ms · query {result.queryId}
              </div>
            </div>
          </div>
        </motion.section>

        {/* Primary matches */}
        <section className="mb-12">
          <header className="flex items-end justify-between mb-5">
            <div>
              <h2 className="text-xl md:text-2xl font-bold text-bonvoy-ink">
                Top matches for your photo
              </h2>
              <p className="text-sm text-bonvoy-slate mt-1">
                Ranked by visual + caption similarity. Hover for the AI's reasoning.
              </p>
            </div>
            <span className="text-xs text-bonvoy-slate">
              {result.primaryMatches.length} results
            </span>
          </header>
          <div className="grid grid-cols-1 gap-5">
            {result.primaryMatches.map((match, i) => (
              <MatchCard key={match.propertyCode} match={match} index={i} primary />
            ))}
          </div>
        </section>

        {/* Nearby in location */}
        {result.nearbyInLocation && result.nearbyInLocation.properties.length > 0 && (
          <section>
            <header className="flex items-end justify-between mb-5">
              <div>
                <h2 className="text-xl md:text-2xl font-bold text-bonvoy-ink">
                  Other properties in {result.nearbyInLocation.city}
                </h2>
                <p className="text-sm text-bonvoy-slate mt-1">
                  Same destination, different vibe — anchored on your top match.
                </p>
              </div>
              <span className="text-xs text-bonvoy-slate">
                {result.nearbyInLocation.properties.length} options
              </span>
            </header>
            <div className="rail flex gap-5 overflow-x-auto pb-3 -mx-1 px-1">
              {result.nearbyInLocation.properties.map((p, i) => (
                <NearbyCard key={p.propertyCode} property={p} index={i} />
              ))}
            </div>
          </section>
        )}
      </main>

      <BonvoyFooter />
    </div>
  );
}
