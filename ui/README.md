# Snap2Stay UI

React + TypeScript + Vite frontend for Snap2Stay visual property search.

## Quick Start

```bash
# Install dependencies
npm install

# Start dev server (connects to backend at localhost:8081)
npm run dev
```

The UI will be available at `http://localhost:5173`.

## Configuration

Edit `.env` to configure:

```bash
# API endpoint
VITE_API_BASE_URL=http://localhost:8081/v1

# Use mock data (for UI development without backend)
VITE_USE_MOCK=false
```

## Backend Requirements

Before using the real API (VITE_USE_MOCK=false), ensure these services are running:

1. **embedding-service** (port 8082) - SigLIP model for image embeddings
2. **content-server** (port 8083) - Serves property images
3. **visual-search-api** (port 8081) - Main search API
4. **image-ingestion-service** - Run once to seed the vector store

## Features

- **Visual Search**: Upload any photo to find matching Marriott properties
- **Location Consent**: Opt-in to use photo GPS data for better results
- **AI Tags**: See what the AI detected in your photo
- **Nearby Properties**: Discover other properties in the same destination
- **Match Scores**: Visual similarity scores with AI explanations

## Tech Stack

- React 18 + TypeScript
- Vite for fast dev/build
- Tailwind CSS for styling
- Framer Motion for animations
- React Router for navigation
