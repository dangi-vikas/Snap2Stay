import { Upload, X, Image as ImageIcon, Sparkles, MapPin } from 'lucide-react';
import { AnimatePresence, motion } from 'framer-motion';
import { useCallback, useEffect, useRef, useState } from 'react';
import { cn } from '@/lib/cn';
import { useSearch } from '@/context/SearchContext';
import { visualSearch } from '@/lib/api';

interface UploadModalProps {
  open: boolean;
  onClose: () => void;
  /** Called once results are ready; parent can navigate to /results. */
  onResults: () => void;
}

export function UploadModal({ open, onClose, onResults }: UploadModalProps) {
  const { setUpload, setResult, setAnalyzing, setError, isAnalyzing, uploadedPreview, error } = useSearch();
  const [isDragging, setIsDragging] = useState(false);
  const [shownTags, setShownTags] = useState<string[]>([]);
  const [useImageLocation, setUseImageLocation] = useState(false);
  const inputRef = useRef<HTMLInputElement>(null);

  // ESC closes the modal (when not in the middle of analyzing).
  useEffect(() => {
    if (!open) return;
    const onKey = (e: KeyboardEvent) => {
      if (e.key === 'Escape' && !isAnalyzing) onClose();
    };
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [open, isAnalyzing, onClose]);

  const submit = useCallback(
    async (file: File) => {
      setUpload(file);
      setAnalyzing(true);
      setShownTags([]);
      try {
        const result = await visualSearch(file, { useImageLocation });

        // Stagger the tags in to feel like the AI is "writing" them.
        const tags = result.debug?.tags ?? [];
        for (let i = 0; i < tags.length; i++) {
          setTimeout(() => setShownTags((prev) => [...prev, tags[i]]), 250 + i * 220);
        }

        // Hold the analyzing state long enough for the tags to read,
        // then transition to results.
        const totalTagDelay = 250 + tags.length * 220 + 350;
        setTimeout(() => {
          setResult(result);
          onResults();
        }, Math.max(800, totalTagDelay));
      } catch (e) {
        setError(e instanceof Error ? e.message : 'Search failed');
      }
    },
    [setUpload, setAnalyzing, setResult, setError, onResults, useImageLocation],
  );

  const handleFiles = useCallback(
    (files: FileList | null) => {
      if (!files || files.length === 0) return;
      const file = files[0];
      if (!file.type.startsWith('image/')) {
        setError('Please upload an image (JPEG or PNG).');
        return;
      }
      if (file.size > 10 * 1024 * 1024) {
        setError('Image too large — max 10 MB.');
        return;
      }
      void submit(file);
    },
    [submit, setError],
  );

  const handleDrop = useCallback(
    (e: React.DragEvent<HTMLDivElement>) => {
      e.preventDefault();
      setIsDragging(false);
      handleFiles(e.dataTransfer.files);
    },
    [handleFiles],
  );

  return (
    <AnimatePresence>
      {open && (
        <motion.div
          initial={{ opacity: 0 }}
          animate={{ opacity: 1 }}
          exit={{ opacity: 0 }}
          transition={{ duration: 0.2 }}
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          {/* Backdrop */}
          <motion.button
            aria-label="Close upload"
            initial={{ opacity: 0 }}
            animate={{ opacity: 1 }}
            exit={{ opacity: 0 }}
            disabled={isAnalyzing}
            onClick={onClose}
            className="absolute inset-0 bg-black/60 backdrop-blur-md"
          />

          <motion.div
            initial={{ scale: 0.92, opacity: 0, y: 20 }}
            animate={{ scale: 1, opacity: 1, y: 0 }}
            exit={{ scale: 0.95, opacity: 0, y: 10 }}
            transition={{ type: 'spring', stiffness: 280, damping: 26 }}
            className="relative bg-white rounded-2xl shadow-2xl w-full max-w-2xl
                       overflow-hidden border border-bonvoy-line"
            onClick={(e) => e.stopPropagation()}
          >
            <div className="px-7 py-5 border-b border-bonvoy-line flex items-start justify-between gap-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <Sparkles className="w-4 h-4 text-snap-glow" />
                  <span className="text-xs tracking-[0.3em] uppercase font-semibold text-snap-glow">
                    Snap2Stay
                  </span>
                </div>
                <h2 className="text-2xl font-bold text-bonvoy-ink">Find Your Marriott</h2>
                <p className="text-sm text-bonvoy-slate mt-1">
                  Upload a photo from a magazine, Pinterest, or your camera roll. Our AI finds the matching property — and others nearby.
                </p>
              </div>
              {!isAnalyzing && (
                <button
                  onClick={onClose}
                  className="text-bonvoy-mist hover:text-bonvoy-ink transition-colors"
                  aria-label="Close"
                >
                  <X className="w-5 h-5" />
                </button>
              )}
            </div>

            <div className="p-7">
              {!uploadedPreview && !isAnalyzing && (
                <>
                  <DropZone
                    isDragging={isDragging}
                    setDragging={setIsDragging}
                    onDrop={handleDrop}
                    onPickFile={() => inputRef.current?.click()}
                  />
                  <LocationConsent 
                    checked={useImageLocation} 
                    onChange={setUseImageLocation} 
                  />
                </>
              )}

              {(uploadedPreview || isAnalyzing) && (
                <AnalyzingPanel preview={uploadedPreview} tags={shownTags} />
              )}

              <input
                ref={inputRef}
                type="file"
                accept="image/jpeg,image/png,image/webp"
                className="hidden"
                onChange={(e) => handleFiles(e.target.files)}
              />

              {error && (
                <motion.div
                  initial={{ opacity: 0, y: -8 }}
                  animate={{ opacity: 1, y: 0 }}
                  className="mt-4 rounded-lg bg-red-50 border border-red-200 px-4 py-3 text-sm text-red-800"
                >
                  {error}
                </motion.div>
              )}
            </div>

            <div className="px-7 py-4 bg-bonvoy-surface text-xs text-bonvoy-slate">
              Your photo stays private. EXIF/GPS metadata is stripped before processing — location is only used if you opt in above, and is never stored.
            </div>
          </motion.div>
        </motion.div>
      )}
    </AnimatePresence>
  );
}

interface DropZoneProps {
  isDragging: boolean;
  setDragging: (v: boolean) => void;
  onDrop: (e: React.DragEvent<HTMLDivElement>) => void;
  onPickFile: () => void;
}

function DropZone({ isDragging, setDragging, onDrop, onPickFile }: DropZoneProps) {
  return (
    <motion.div
      onDragOver={(e) => {
        e.preventDefault();
        setDragging(true);
      }}
      onDragLeave={() => setDragging(false)}
      onDrop={onDrop}
      onClick={onPickFile}
      animate={{ scale: isDragging ? 1.02 : 1 }}
      className={cn(
        'relative border-2 border-dashed rounded-xl p-12',
        'flex flex-col items-center justify-center gap-3',
        'cursor-pointer transition-colors duration-200',
        isDragging
          ? 'border-snap-glow bg-snap-glow/5'
          : 'border-bonvoy-line hover:border-bonvoy-gold hover:bg-bonvoy-surface',
      )}
    >
      <motion.div
        animate={{ y: isDragging ? -4 : 0 }}
        className="w-14 h-14 rounded-full bg-gradient-to-br from-snap-glow to-bonvoy-gold
                   flex items-center justify-center text-white shadow-lg"
      >
        <Upload className="w-6 h-6" strokeWidth={2.4} />
      </motion.div>
      <div className="text-center">
        <p className="text-base font-semibold text-bonvoy-ink">
          {isDragging ? 'Drop your photo here' : 'Drag a photo or click to upload'}
        </p>
        <p className="text-xs text-bonvoy-slate mt-1">JPEG, PNG, or WebP — up to 10 MB</p>
      </div>
      <div className="flex gap-2 mt-2">
        {['Pinterest screenshot', 'Magazine clipping', 'Vacation photo'].map((label) => (
          <span key={label} className="tag-chip">
            <ImageIcon className="w-3 h-3" />
            {label}
          </span>
        ))}
      </div>
    </motion.div>
  );
}

interface AnalyzingPanelProps {
  preview: string | null;
  tags: string[];
}

function AnalyzingPanel({ preview, tags }: AnalyzingPanelProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-[1.1fr_1fr] gap-5">
      <div className="relative rounded-xl overflow-hidden bg-bonvoy-line aspect-[4/3]">
        {preview && (
          <img src={preview} alt="Uploaded" className="w-full h-full object-cover" />
        )}
        {/* Shimmer overlay while we wait */}
        <div className="absolute inset-0 shimmer pointer-events-none" />
        {/* Analyzing pill */}
        <div className="absolute top-3 left-3 flex items-center gap-2 bg-white/95 backdrop-blur-sm rounded-full px-3 py-1.5 shadow-md">
          <span className="relative flex h-2 w-2">
            <span className="animate-ping absolute inline-flex h-full w-full rounded-full bg-snap-glow opacity-75" />
            <span className="relative inline-flex rounded-full h-2 w-2 bg-snap-glow" />
          </span>
          <span className="text-xs font-semibold text-bonvoy-ink">Analyzing...</span>
        </div>
      </div>

      <div className="flex flex-col">
        <div className="flex items-center gap-2 mb-3">
          <Sparkles className="w-4 h-4 text-snap-glow" />
          <h3 className="text-sm font-semibold text-bonvoy-ink">What we see</h3>
        </div>
        <div className="flex flex-wrap gap-2 min-h-[120px]">
          <AnimatePresence>
            {tags.map((tag) => (
              <motion.span
                key={tag}
                initial={{ scale: 0.5, opacity: 0, y: 8 }}
                animate={{ scale: 1, opacity: 1, y: 0 }}
                exit={{ scale: 0.5, opacity: 0 }}
                transition={{ type: 'spring', stiffness: 320, damping: 18 }}
                className="tag-chip border-snap-glow/30 bg-snap-glow/10 text-bonvoy-ink"
              >
                {tag}
              </motion.span>
            ))}
          </AnimatePresence>
          {tags.length === 0 && (
            <p className="text-xs text-bonvoy-mist italic">
              Reading visual features...
            </p>
          )}
        </div>
        <div className="mt-auto pt-4 text-xs text-bonvoy-slate space-y-1.5">
          <Step done>Image preprocessed (EXIF stripped)</Step>
          <Step done={tags.length > 0}>Visual fingerprint generated</Step>
          <Step done={tags.length > 2} pending={tags.length > 0 && tags.length <= 2}>
            Matching against {ESTIMATED_PROPERTY_COUNT}+ Marriott properties
          </Step>
          <Step pending={tags.length > 2}>Compiling results</Step>
        </div>
      </div>
    </div>
  );
}

const ESTIMATED_PROPERTY_COUNT = '8,000';

function Step({ children, done, pending }: { children: React.ReactNode; done?: boolean; pending?: boolean }) {
  return (
    <div className="flex items-center gap-2">
      <span
        className={cn(
          'w-1.5 h-1.5 rounded-full',
          done ? 'bg-snap-glow' : pending ? 'bg-snap-glow/50 animate-pulse' : 'bg-bonvoy-line',
        )}
      />
      <span className={done ? 'text-bonvoy-ink' : 'text-bonvoy-slate'}>{children}</span>
    </div>
  );
}

interface LocationConsentProps {
  checked: boolean;
  onChange: (checked: boolean) => void;
}

function LocationConsent({ checked, onChange }: LocationConsentProps) {
  return (
    <motion.div
      initial={{ opacity: 0, y: 8 }}
      animate={{ opacity: 1, y: 0 }}
      transition={{ delay: 0.1 }}
      className="mt-4 p-4 rounded-xl bg-bonvoy-surface border border-bonvoy-line"
    >
      <label className="flex items-start gap-3 cursor-pointer group">
        <div className="relative flex items-center justify-center mt-0.5">
          <input
            type="checkbox"
            checked={checked}
            onChange={(e) => onChange(e.target.checked)}
            className="peer sr-only"
          />
          <div
            className={cn(
              'w-5 h-5 rounded border-2 transition-all duration-200',
              'flex items-center justify-center',
              checked
                ? 'bg-snap-glow border-snap-glow'
                : 'border-bonvoy-mist group-hover:border-bonvoy-gold',
            )}
          >
            {checked && (
              <motion.svg
                initial={{ scale: 0 }}
                animate={{ scale: 1 }}
                className="w-3 h-3 text-white"
                viewBox="0 0 12 12"
                fill="none"
                stroke="currentColor"
                strokeWidth={2.5}
                strokeLinecap="round"
                strokeLinejoin="round"
              >
                <path d="M2 6l3 3 5-6" />
              </motion.svg>
            )}
          </div>
        </div>
        <div className="flex-1">
          <div className="flex items-center gap-2">
            <MapPin className="w-4 h-4 text-snap-glow" />
            <span className="text-sm font-medium text-bonvoy-ink">
              Use photo location to improve results
            </span>
          </div>
          <p className="text-xs text-bonvoy-slate mt-1 leading-relaxed">
            If your photo has GPS data, we'll prioritize properties near where it was taken.
            Your location is used only for this search and is never stored.
          </p>
        </div>
      </label>
    </motion.div>
  );
}
