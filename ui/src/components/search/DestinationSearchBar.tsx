import { ChevronDown, MapPin, Calendar, Search } from 'lucide-react';
import { motion } from 'framer-motion';
import { useState } from 'react';

/**
 * Search bar floating over the hero. Matches the Marriott Bonvoy
 * landing page screenshot: Destination | Dates | Find Hotels button on top row,
 * Rooms & Guests | Special Rates | Use Points/Awards on bottom row.
 */
export function DestinationSearchBar() {
  const [destination, setDestination] = useState('');
  const [dates, setDates] = useState('Sun, May 17 - Mon, May 18');
  
  return (
    <motion.div
      initial={{ y: 24, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ delay: 0.2, duration: 0.6, ease: [0.22, 1, 0.36, 1] }}
      className="bg-white rounded-lg shadow-xl"
    >
      <div className="flex flex-col md:flex-row">
        <div className="flex-1 px-5 py-5 border-b md:border-b-0 md:border-r border-gray-200">
          <div className="flex items-center gap-2 mb-2">
            <MapPin className="w-3 h-3 text-orange-500" />
            <span className="text-[13px] font-semibold uppercase tracking-wider text-gray-500">Destination</span>
          </div>
          <input 
            type="text"
            value={destination}
            onChange={(e) => setDestination(e.target.value)}
            className="w-full text-xl font-medium text-gray-900 outline-none bg-transparent placeholder-gray-400"
            placeholder="Where can we take you?"
          />
        </div>

        <div className="flex-1 px-5 py-5 border-b md:border-b-0">
          <div className="flex items-center gap-2 mb-2">
            <Calendar className="w-3 h-3 text-orange-500" />
            <span className="text-[13px] font-semibold uppercase tracking-wider text-gray-500">1 Night</span>
          </div>
          <input 
            type="text"
            value={dates}
            onChange={(e) => setDates(e.target.value)}
            className="w-full text-xl font-medium text-gray-900 outline-none bg-transparent placeholder-gray-400"
            placeholder="Select dates"
          />
        </div>

        <div className="px-4 py-4 flex items-center">
          <button className="bg-black text-white rounded-full px-28 py-4 font-medium hover:bg-gray-800 transition-colors flex items-center gap-2 whitespace-nowrap text-base">
            <Search className="w-5 h-5" />
            Find Hotels
          </button>
        </div>
      </div>

      <div className="border-t border-gray-200 px-5 py-4 flex flex-wrap items-center gap-6">
        <button className="flex items-center gap-2 text-sm text-gray-700 hover:text-gray-900 transition-colors">
          <span>1 Room, 1 Guest</span>
          <ChevronDown className="w-3.5 h-3.5 text-gray-500" />
        </button>
        <button className="flex items-center gap-2 text-sm text-gray-700 hover:text-gray-900 transition-colors">
          <span>Lowest Regular Rate</span>
          <ChevronDown className="w-3.5 h-3.5 text-gray-500" />
        </button>
        <label className="flex items-center gap-2 text-sm text-gray-700 cursor-pointer">
          <input
            type="checkbox"
            className="w-4 h-4 rounded border-gray-300"
          />
          Use Points/Awards
        </label>
      </div>
    </motion.div>
  );
}
