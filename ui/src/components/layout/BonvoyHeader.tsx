import { Globe, HelpCircle, Briefcase } from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/cn';

/**
 * Snap2Stay header. Clean minimal nav with branding, matching the app's
 * orange-gold gradient palette.
 *
 * The dark/light variant prop lets us swap colors based on whether the page
 * has a hero image background (transparent) or a normal white background (solid).
 */
interface BonvoyHeaderProps {
  variant?: 'transparent' | 'solid';
}

const NAV_ITEMS = ['Book', 'Offers', 'Brands', 'Credit Cards', 'Marriott Bonvoy', 'Meetings & Events'];

export function BonvoyHeader({ variant = 'transparent' }: BonvoyHeaderProps) {
  const isLight = variant === 'transparent';
  return (
    <header
      className={cn(
        'group transition-colors duration-300',
        isLight
          ? 'absolute top-0 left-0 right-0 z-30 bg-black/20 backdrop-blur-sm text-white hover:bg-white hover:text-bonvoy-ink'
          : 'sticky top-0 z-30 bg-white border-b border-bonvoy-line text-bonvoy-ink'
      )}
    >
      <div className="mx-auto max-w-[1280px] px-6 py-2 flex items-center justify-between gap-6">
        <Link to="/" className="flex items-center" aria-label="Snap2Stay home">
          <Snap2StayLogo light={isLight} />
        </Link>

        <nav className="hidden md:flex items-center gap-6 text-sm font-medium">
          {NAV_ITEMS.map((item) => (
            <a
              key={item}
              href="#"
              className={cn(
                'transition-colors',
                isLight ? 'group-hover:text-bonvoy-ink hover:text-bonvoy-gold-soft' : 'hover:text-bonvoy-gold-soft'
              )}
            >
              {item}
            </a>
          ))}
        </nav>

        <div className="flex items-center gap-5 text-sm">
          <button className={cn(
            'flex items-center gap-1.5 transition-colors',
            isLight ? 'group-hover:text-bonvoy-ink hover:text-bonvoy-gold-soft' : 'hover:text-bonvoy-gold-soft'
          )}>
            <HelpCircle className="w-4 h-4" />
            Help
          </button>
          <button className={cn(
            'hidden sm:flex items-center gap-1.5 transition-colors',
            isLight ? 'group-hover:text-bonvoy-ink hover:text-bonvoy-gold-soft' : 'hover:text-bonvoy-gold-soft'
          )}>
            <Globe className="w-4 h-4" />
            English
          </button>
          <button className={cn(
            'hidden sm:flex items-center gap-1.5 transition-colors',
            isLight ? 'group-hover:text-bonvoy-ink hover:text-bonvoy-gold-soft' : 'hover:text-bonvoy-gold-soft'
          )}>
            <Briefcase className="w-4 h-4" />
            Trips
          </button>
          <button
            className={cn(
              'rounded-full px-5 py-2 transition-colors duration-200 font-medium',
              isLight
                ? 'border border-white text-white group-hover:border-bonvoy-ink group-hover:text-bonvoy-ink hover:!bg-bonvoy-ink hover:!text-white'
                : 'border border-bonvoy-ink/40 text-bonvoy-ink hover:bg-bonvoy-ink hover:text-white'
            )}
          >
            Sign In or Join
          </button>
        </div>
      </div>
    </header>
  );
}

function Snap2StayLogo({ light }: { light: boolean }) {
  return (
    <div className="flex items-center gap-0.5">
      <img
        src="/snap2stay-icon.png"
        alt="Snap2Stay"
        className="h-16 w-auto select-none mt-2.5"
        draggable={false}
      />
      <div className="flex flex-col leading-tight">
        <span
          className={cn(
            'text-[14px] tracking-wide transition-colors',
            light ? 'text-white/70 group-hover:text-snap-glow' : 'text-snap-glow'
          )}
          style={{ fontFamily: '"Playfair Display", serif', fontStyle: 'italic', fontWeight: 400 }}
        >
          Snap2Stay
        </span>
        <span
          className={cn(
            'text-[26px] font-semibold tracking-[0.08em] transition-colors -mt-0.5',
            light ? 'text-white group-hover:text-bonvoy-ink' : 'text-bonvoy-ink'
          )}
          style={{ fontFamily: '"Playfair Display", serif', fontStyle: 'italic', fontWeight: 600 }}
        >
          Marriott
        </span>
      </div>
    </div>
  );
}