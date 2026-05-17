import type { SearchOptions, VisualSearchResponseWithDebug } from '@/types/api';
import { mockMaldivesResponse } from './mocks';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/v1';
const USE_MOCK = (import.meta.env.VITE_USE_MOCK ?? 'true') === 'true';

const MOCK_LATENCY_MS = 1800;

/**
 * Visual search client. By default runs against mocks (codefest demo safety);
 * set VITE_USE_MOCK=false to hit the real visual-search-api.
 *
 * Flip the switch only when:
 *   1. embedding-service has CLIP weights cached
 *   2. content-server is up and seeded
 *   3. ingestion has populated the vector store
 *   (i.e. when `docker compose up` is healthy)
 */
export async function visualSearch(
  image: File,
  options: SearchOptions = {},
): Promise<VisualSearchResponseWithDebug> {
  if (USE_MOCK) {
    return mockSearch(image, options);
  }
  return realSearch(image, options);
}

async function mockSearch(
  _image: File,
  _options: SearchOptions,
): Promise<VisualSearchResponseWithDebug> {
  await sleep(MOCK_LATENCY_MS);
  return mockMaldivesResponse;
}

async function realSearch(
  image: File,
  options: SearchOptions,
): Promise<VisualSearchResponseWithDebug> {
  const formData = new FormData();
  formData.append('image', image);
  if (options.useImageLocation !== undefined) {
    formData.append('useImageLocation', String(options.useImageLocation));
  }
  if (options.destination) {
    formData.append('destination', options.destination);
  }
  if (options.checkIn) formData.append('checkIn', options.checkIn);
  if (options.checkOut) formData.append('checkOut', options.checkOut);
  if (options.filters?.brand?.length) {
    formData.append('brand', options.filters.brand.join(','));
  }
  if (options.filters?.maxPriceUSD !== undefined) {
    formData.append('maxPriceUSD', String(options.filters.maxPriceUSD));
  }
  if (options.filters?.marketCode) {
    formData.append('marketCode', options.filters.marketCode);
  }

  const res = await fetch(`${API_BASE}/visual-search`, {
    method: 'POST',
    body: formData,
  });
  if (!res.ok) {
    const text = await res.text().catch(() => '');
    throw new VisualSearchError(res.status, text || res.statusText);
  }
  return res.json();
}

export class VisualSearchError extends Error {
  constructor(public status: number, message: string) {
    super(message);
    this.name = 'VisualSearchError';
  }
}

const sleep = (ms: number) => new Promise<void>((resolve) => setTimeout(resolve, ms));

export const isUsingMock = USE_MOCK;
