export function BonvoyFooter() {
  return (
    <footer className="bg-bonvoy-ink text-white/80 text-xs py-8 px-6 mt-auto">
      <div className="mx-auto max-w-[1280px] flex flex-wrap items-center justify-between gap-4">
        <div>
          © {new Date().getFullYear()} Marriott International, Inc. —{' '}
          <span style={{ fontFamily: '"Playfair Display", serif', fontStyle: 'italic' }}>Snap2Stay</span>{' '}
          (Codefest 4.0)
        </div>
        <div className="flex gap-5">
          <a href="#" className="hover:text-white transition-colors">Privacy Center</a>
          <a href="#" className="hover:text-white transition-colors">Terms of Use</a>
          <a href="#" className="hover:text-white transition-colors">Site Map</a>
        </div>
      </div>
    </footer>
  );
}
