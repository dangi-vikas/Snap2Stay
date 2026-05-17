import { motion } from 'framer-motion';

export function HeroOffer() {
  return (
    <motion.div
      initial={{ y: 32, opacity: 0 }}
      animate={{ y: 0, opacity: 1 }}
      transition={{ delay: 0.5, duration: 0.7, ease: [0.22, 1, 0.36, 1] }}
      className="text-white"
    >
      <p className="text-xs tracking-[0.3em] font-medium mb-4 text-white/90 uppercase">
        Limited Time Offer!
      </p>
      <h1 className="text-4xl md:text-5xl font-light mb-4">
        Stay. Spend. Savor in Thailand
      </h1>
      <p className="text-base text-white/90 mb-6 max-w-md">
        Earn more at Thailand's finest hotels & resorts
      </p>
      <button className="rounded-full bg-white text-black px-7 py-3 text-sm font-medium hover:bg-gray-100 transition-colors">
        Explore Now
      </button>
    </motion.div>
  );
}
