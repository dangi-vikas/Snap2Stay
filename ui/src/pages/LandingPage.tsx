import { useNavigate } from 'react-router-dom';
import { useState } from 'react';
import { BonvoyHeader } from '@/components/layout/BonvoyHeader';
import { DestinationSearchBar } from '@/components/search/DestinationSearchBar';
import { HeroOffer } from '@/components/search/HeroOffer';
import { CameraFab } from '@/components/camera/CameraFab';
import { UploadModal } from '@/components/camera/UploadModal';

const HERO_IMAGE = '/hero-bg.png';

export function LandingPage() {
  const [uploadOpen, setUploadOpen] = useState(false);
  const navigate = useNavigate();

  return (
    <div className="relative min-h-screen flex flex-col">
      <BonvoyHeader variant="transparent" />

      {/* Hero with full-bleed image */}
      <div className="relative flex-1 min-h-[640px]">
        <div className="absolute inset-0 z-0">
          <img
            src={HERO_IMAGE}
            alt="Luxury resort with pool and private cabanas"
            className="w-full h-full object-cover"
          />
        </div>

        {/* Content container with consistent max-width */}
        <div className="relative z-20 mx-auto max-w-[1080px] px-6">
          {/* Search bar — floats below the header with gap */}
          <div className="pt-32">
            <DestinationSearchBar />
          </div>

          {/* Hero copy aligned with search bar */}
          <div className="pt-24 pb-16">
            <HeroOffer />
          </div>
        </div>
      </div>

      <CameraFab onClick={() => setUploadOpen(true)} pulsing={!uploadOpen} />

      <UploadModal
        open={uploadOpen}
        onClose={() => setUploadOpen(false)}
        onResults={() => {
          setUploadOpen(false);
          // Brief delay so modal close animation completes before route change.
          setTimeout(() => navigate('/results'), 150);
        }}
      />
    </div>
  );
}
