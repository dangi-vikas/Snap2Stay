/**
 * Types mirror Snap2Stay/openapi/visual-search-api.yaml exactly.
 * If the OpenAPI changes, change these. Do not let them drift.
 */

export interface PropertyMatch {
  propertyCode: string;
  name: string;
  brand?: string;
  city?: string;
  marketCode?: string;
  matchScore: number;       // 0..1
  thumbnailUrl: string;
  explanation?: string;
  available: boolean;
}

export interface NearbyGroup {
  city: string;
  marketCode?: string;
  anchorPropertyCode: string;
  properties: PropertyMatch[];
}

export interface VisualSearchResponse {
  primaryMatches: PropertyMatch[];
  nearbyInLocation?: NearbyGroup;
  queryId: string;
  tookMs: number;
}

export interface SearchFilters {
  brand?: string[];
  maxPriceUSD?: number;
  marketCode?: string;
}

export interface SearchOptions {
  useImageLocation?: boolean;
  destination?: string;
  filters?: SearchFilters;
  checkIn?: string;
  checkOut?: string;
}

/**
 * Client-side enrichment: tags + caption come back from the embedding service
 * via the API. We surface them in the UI to make the AI's reasoning visible.
 * NOTE: not in the current OpenAPI but trivially addable; the codefest demo
 * benefits from showing them, so we expose them as an optional field.
 */
export interface VisualSearchResponseWithDebug extends VisualSearchResponse {
  debug?: {
    tags?: string[];
    caption?: string;
  };
}
