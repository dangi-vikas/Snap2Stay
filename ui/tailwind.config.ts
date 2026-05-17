import type { Config } from 'tailwindcss';

/**
 * Marriott Bonvoy-inspired tokens. Pulled from observable brand cues:
 * cream/off-white surfaces, deep charcoal text, gold accent for CTAs,
 * subtle warm grays for borders.
 */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        bonvoy: {
          ink: '#1a1a1a',
          charcoal: '#2c2c2c',
          slate: '#5a5a5a',
          mist: '#8a8a8a',
          line: '#e5e3df',
          surface: '#f7f5f1',
          paper: '#ffffff',
          gold: '#a37e3a',
          'gold-soft': '#c9a363',
          accent: '#1a1a1a',
        },
        snap: {
          glow: '#ff6b35',
          'glow-soft': '#ff8c5f',
        },
      },
      fontFamily: {
        sans: ['"Proxima Nova"', '"Helvetica Neue"', 'Helvetica', 'Arial', 'system-ui', 'sans-serif'],
        serif: ['Georgia', '"Times New Roman"', 'serif'],
      },
      boxShadow: {
        card: '0 1px 3px rgba(0,0,0,0.04), 0 4px 16px rgba(0,0,0,0.05)',
        'card-hover': '0 4px 12px rgba(0,0,0,0.08), 0 12px 32px rgba(0,0,0,0.10)',
        fab: '0 8px 24px rgba(255,107,53,0.35), 0 2px 8px rgba(0,0,0,0.15)',
        'fab-hover': '0 12px 32px rgba(255,107,53,0.5), 0 4px 12px rgba(0,0,0,0.2)',
      },
      backdropBlur: {
        xs: '2px',
      },
      keyframes: {
        'pulse-ring': {
          '0%': { transform: 'scale(1)', opacity: '0.6' },
          '100%': { transform: 'scale(2.4)', opacity: '0' },
        },
        'shimmer': {
          '0%': { backgroundPosition: '-200% 0' },
          '100%': { backgroundPosition: '200% 0' },
        },
        'pop-in': {
          '0%': { transform: 'scale(0.5) translateY(8px)', opacity: '0' },
          '60%': { transform: 'scale(1.06) translateY(0)', opacity: '1' },
          '100%': { transform: 'scale(1) translateY(0)', opacity: '1' },
        },
        'progress-fill': {
          '0%': { width: '0%' },
          '100%': { width: 'var(--target-width, 100%)' },
        },
      },
      animation: {
        'pulse-ring': 'pulse-ring 2s ease-out infinite',
        'pulse-ring-delay': 'pulse-ring 2s ease-out 1s infinite',
        'shimmer': 'shimmer 2s linear infinite',
        'pop-in': 'pop-in 400ms cubic-bezier(0.34, 1.56, 0.64, 1) both',
      },
    },
  },
  plugins: [],
} satisfies Config;
