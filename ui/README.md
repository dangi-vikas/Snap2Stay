# Snap2Stay UI

Marriott Bonvoy-styled React + TypeScript front-end for the Snap2Stay visual property search.

## Stack

- **Vite 5** + **React 18** + **TypeScript 5**
- **Tailwind CSS** (with Marriott Bonvoy brand tokens in `tailwind.config.ts`)
- **Framer Motion** for page transitions and choreographed entrance animations
- **react-router-dom v6** for `/` (landing) → `/results`
- **lucide-react** icons

## Layout

```
src/
├── main.tsx                 # entry
├── App.tsx                  # BrowserRouter + AnimatePresence
├── styles/index.css         # Tailwind + global components (.btn-primary, .glass-panel, etc.)
├── lib/
│   ├── api.ts               # visualSearch() — flips between mock and real
│   ├── mocks.ts             # canned demo response (Maldives example)
│   └── cn.ts                # clsx wrapper
├── types/api.ts             # mirrors openapi/visual-search-api.yaml
├── context/SearchContext.tsx
├── components/
│   ├── layout/              # BonvoyHeader, BonvoyFooter
│   ├── search/              # DestinationSearchBar, HeroOffer
│   ├── camera/              # CameraFab, UploadModal
│   └── results/             # MatchCard, NearbyCard, ScoreBar
└── pages/
    ├── LandingPage.tsx      # mirrors the Marriott landing screenshot
    └── ResultsPage.tsx      # two rails: top matches + nearby-in-location
```

## Run

```bash
cd Snap2Stay/ui
npm install
npm run dev      # http://localhost:5173
```

## Mock vs real backend

By default `VITE_USE_MOCK=true` (see `.env`), so the UI returns a canned
"Maldives overwater villa" response after a fake 1.8 s delay. This makes
the demo bullet-proof — no backend needed.

To hit the real `visual-search-api`:

```bash
# 1. start backend services
cd ..
docker compose up

# 2. point UI at it
echo 'VITE_USE_MOCK=false' > Snap2Stay/ui/.env.local
npm run dev
```

The Vite dev server proxies `/v1/*` → `http://localhost:8081`, so CORS is a
non-issue locally.

## Key UX decisions

| Decision | Why |
|---|---|
| Camera FAB (bottom-right) instead of replacing the destination search | Per design call: keep the Marriott landing page intact, surface Snap2Stay as an additive entry point with a radiating ripple to draw the eye. |
| Two distinct rails on results page | Mirrors the architecture's `primaryMatches` (visual+text scored) vs `nearbyInLocation` (location-pivoted). Judges see *what the AI found* AND *expansion of options*. |
| Streaming AI tags during analyze | Makes the multimodal model's reasoning visible — shows zero-shot classification working in real time. |
| EXIF privacy notice in modal footer | Reinforces the privacy invariant from the architecture doc directly to the guest. |

## Verifying

```bash
npm run lint     # tsc --noEmit
npm run build    # type-check + production bundle
```
