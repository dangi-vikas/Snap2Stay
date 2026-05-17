import type { VisualSearchResponseWithDebug } from '@/types/api';

/**
 * Mocked response used when VITE_USE_MOCK=true (default for codefest dev).
 * Mirrors what the real visual-search-api would return for a "Maldives overwater
 * villa" photo — top match plus 2 same-market nearby properties.
 *
 * Thumbnails point at Unsplash hotlinks so the demo works offline-ish without
 * the content-server seed images being in place yet.
 */
export const mockMaldivesResponse: VisualSearchResponseWithDebug = {
  queryId: 'qry_mock_mlewh',
  tookMs: 412,
  primaryMatches: [
    {
      propertyCode: 'MLEWH',
      name: 'W Maldives',
      brand: 'W Hotels',
      city: 'Maldives',
      marketCode: 'MLE',
      matchScore: 0.91,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Overwater villa with infinity pool at sunset',
      available: true,
    },
    {
      propertyCode: 'MLEAK',
      name: 'JW Marriott Maldives Resort & Spa',
      brand: 'JW Marriott',
      city: 'Maldives',
      marketCode: 'MLE',
      matchScore: 0.84,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Beachfront luxury suite, similar architectural style',
      available: true,
    },
    {
      propertyCode: 'MLERZ',
      name: 'The Ritz-Carlton Maldives, Fari Islands',
      brand: 'Ritz-Carlton',
      city: 'Maldives',
      marketCode: 'MLE',
      matchScore: 0.78,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Lagoon-side overwater bungalow',
      available: false,
    },
  ],
  nearbyInLocation: {
    city: 'Maldives',
    marketCode: 'MLE',
    anchorPropertyCode: 'MLEWH',
    properties: [
      {
        propertyCode: 'MLESH',
        name: 'Sheraton Maldives Full Moon Resort',
        brand: 'Sheraton',
        city: 'Maldives',
        marketCode: 'MLE',
        matchScore: 0,
        thumbnailUrl:
          'https://images.unsplash.com/photo-1551918120-9739cb430c6d?auto=format&fit=crop&w=1200&q=80',
        available: true,
      },
      {
        propertyCode: 'MLELC',
        name: 'Le Méridien Maldives Resort & Spa',
        brand: 'Le Méridien',
        city: 'Maldives',
        marketCode: 'MLE',
        matchScore: 0,
        thumbnailUrl:
          'https://images.unsplash.com/photo-1571896349842-33c89424de2d?auto=format&fit=crop&w=1200&q=80',
        available: true,
      },
      {
        propertyCode: 'MLEWA',
        name: 'The Westin Maldives Miriandhoo Resort',
        brand: 'Westin',
        city: 'Maldives',
        marketCode: 'MLE',
        matchScore: 0,
        thumbnailUrl:
          'https://images.unsplash.com/photo-1582719508461-905c673771fd?auto=format&fit=crop&w=1200&q=80',
        available: true,
      },
    ],
  },
  debug: {
    tags: ['overwater', 'beach', 'tropical', 'infinity-pool', 'sunset'],
    caption: 'overwater bungalow at sunset with infinity pool',
  },
};
