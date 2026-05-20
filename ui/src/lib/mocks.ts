import type { VisualSearchResponseWithDebug } from '@/types/api';

/**
 * Mocked response used when VITE_USE_MOCK=true (default for codefest dev).
 * Mirrors what the real visual-search-api would return for a beach/resort photo.
 *
 * Updated to match the actual seed properties in properties.json.
 */
export const mockBeachResponse: VisualSearchResponseWithDebug = {
  queryId: 'qry_mock_beach',
  tookMs: 387,
  primaryMatches: [
    {
      propertyCode: 'HKTJW',
      name: 'JW Marriott Phuket Resort & Spa',
      brand: 'JW Marriott',
      city: 'Phuket',
      marketCode: 'HKT',
      matchScore: 0.89,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1540541338287-41700207dee6?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Tropical beachfront resort with ocean views',
      available: true,
    },
    {
      propertyCode: 'MRKJW',
      name: 'JW Marriott Marco Island Beach Resort',
      brand: 'JW Marriott',
      city: 'Marco Island',
      marketCode: 'MRK',
      matchScore: 0.82,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1573843981267-be1999ff37cd?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Luxury beach resort with similar tropical aesthetic',
      available: true,
    },
    {
      propertyCode: 'HXWMC',
      name: 'Marriott Hilton Head Resort & Spa',
      brand: 'Marriott',
      city: 'Hilton Head',
      marketCode: 'HXW',
      matchScore: 0.74,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1602002418082-a4443e081dd1?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Family-friendly beach resort',
      available: true,
    },
  ],
  nearbyInLocation: {
    city: 'Phuket',
    marketCode: 'HKT',
    anchorPropertyCode: 'HKTJW',
    properties: [],
  },
  debug: {
    tags: ['beach', 'tropical', 'ocean', 'resort', 'luxury'],
    caption: 'tropical beachfront resort with ocean views',
  },
};

/**
 * Mock response for urban/city photos.
 */
export const mockUrbanResponse: VisualSearchResponseWithDebug = {
  queryId: 'qry_mock_urban',
  tookMs: 342,
  primaryMatches: [
    {
      propertyCode: 'DXBJW',
      name: 'JW Marriott Marquis Hotel Dubai',
      brand: 'JW Marriott',
      city: 'Dubai',
      marketCode: 'DXB',
      matchScore: 0.91,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1512453979798-5ea266f8880c?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Modern urban skyline with luxury amenities',
      available: true,
    },
    {
      propertyCode: 'BNAJW',
      name: 'JW Marriott Nashville',
      brand: 'JW Marriott',
      city: 'Nashville',
      marketCode: 'BNA',
      matchScore: 0.84,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1545893835-abaa50cbe628?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Metropolitan luxury hotel with skyline views',
      available: true,
    },
    {
      propertyCode: 'SGNJW',
      name: 'JW Marriott Hotel Saigon',
      brand: 'JW Marriott',
      city: 'Ho Chi Minh City',
      marketCode: 'SGN',
      matchScore: 0.78,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1583417319070-4a69db38a482?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Urban luxury in a vibrant city',
      available: true,
    },
  ],
  nearbyInLocation: {
    city: 'Dubai',
    marketCode: 'DXB',
    anchorPropertyCode: 'DXBJW',
    properties: [],
  },
  debug: {
    tags: ['urban', 'skyline', 'modern', 'luxury', 'metropolitan'],
    caption: 'modern urban skyline with luxury hotel',
  },
};

/**
 * Mock response for heritage/romantic photos (like Udaipur).
 */
export const mockHeritageResponse: VisualSearchResponseWithDebug = {
  queryId: 'qry_mock_heritage',
  tookMs: 298,
  primaryMatches: [
    {
      propertyCode: 'UDRMR',
      name: 'Udaipur Marriott Resort & Spa',
      brand: 'Marriott',
      city: 'Udaipur',
      marketCode: 'UDR',
      matchScore: 0.93,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1524492412937-b28074a5d7da?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Historic lakeside resort with romantic ambiance',
      available: true,
    },
    {
      propertyCode: 'IXCJW',
      name: 'JW Marriott Chandigarh',
      brand: 'JW Marriott',
      city: 'Chandigarh',
      marketCode: 'IXC',
      matchScore: 0.71,
      thumbnailUrl:
        'https://images.unsplash.com/photo-1566073771259-6a8506099945?auto=format&fit=crop&w=1200&q=80',
      explanation: 'Modern luxury with Indian hospitality',
      available: true,
    },
  ],
  nearbyInLocation: {
    city: 'Udaipur',
    marketCode: 'UDR',
    anchorPropertyCode: 'UDRMR',
    properties: [],
  },
  debug: {
    tags: ['historic', 'romantic', 'luxury', 'lagoon', 'spa'],
    caption: 'historic lakeside resort with romantic setting',
  },
};

// Default export for backward compatibility
export const mockMaldivesResponse = mockBeachResponse;
