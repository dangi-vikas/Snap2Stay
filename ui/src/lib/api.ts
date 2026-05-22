import type { SearchOptions, VisualSearchResponseWithDebug } from '@/types/api';

const API_BASE = import.meta.env.VITE_API_BASE_URL || '/v1';

/**
 * Visual search client. Always hits the real visual-search-api — no mock fallback.
 *
 * The backend uses Google's SigLIP model for 768-dim embeddings with ~10% better
 * accuracy than CLIP. Hybrid search combines visual similarity with auto-generated
 * tags for better results.
 */
export async function visualSearch(
  image: File,
  options: SearchOptions = {},
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
