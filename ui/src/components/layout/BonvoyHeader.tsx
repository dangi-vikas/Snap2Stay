import { Globe, HelpCircle, Briefcase } from 'lucide-react';
import { Link } from 'react-router-dom';
import { cn } from '@/lib/cn';

/**
 * Marriott Bonvoy header. Faithfully matches the screenshot — Book / Offers /
 * Brands / Credit Cards / Marriott Bonvoy / Meetings & Events nav, right-side
 * Help / Language / Trips / Sign In.
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
        <Link to="/" className="flex items-center -mb-2" aria-label="Marriott Bonvoy home">
          <BonvoyLogo light={isLight} />
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

/**
 * Official Marriott Bonvoy logo. 
 * Uses the original PNG logo as-is
 */
function BonvoyLogo({ light }: { light: boolean }) {
  // Always show the original logo without any color changes
  return (
    <img
      src="/marriott-bonvoy-logo.png"
      alt="Marriott Bonvoy"
      className="h-[72px] w-auto select-none"
      draggable={false}
    />
  );
}