
import React, { useState, useEffect, useRef } from 'react';

type ViewState = 'home' | 'privacy' | 'terms' | 'support';

interface NavbarProps {
  currentView: ViewState;
  onNavigate: (view: ViewState) => void;
}

export const Navbar: React.FC<NavbarProps> = ({ currentView, onNavigate }) => {
  const [scrolled, setScrolled] = useState(false);
  const [pendingScrollTarget, setPendingScrollTarget] = useState<string | null>(null);

  useEffect(() => {
    const handleScroll = () => setScrolled(window.scrollY > 20);
    window.addEventListener('scroll', handleScroll);
    return () => window.removeEventListener('scroll', handleScroll);
  }, []);

  // Handle deferred scrolling after navigation to home view
  const prevViewRef = useRef(currentView);
  useEffect(() => {
    if (prevViewRef.current !== 'home' && currentView === 'home' && pendingScrollTarget) {
      const el = document.getElementById(pendingScrollTarget);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
      setPendingScrollTarget(null);
    }
    prevViewRef.current = currentView;
  }, [currentView, pendingScrollTarget]);

  const handleLogoClick = () => {
    onNavigate('home');
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  const handleSectionClick = (e: React.MouseEvent<HTMLAnchorElement>, id: string) => {
    e.preventDefault();
    if (currentView !== 'home') {
      setPendingScrollTarget(id);
      onNavigate('home');
    } else {
      const el = document.getElementById(id);
      if (el) el.scrollIntoView({ behavior: 'smooth' });
    }
  };

  return (
    <nav className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? 'py-4 bg-dark/80 backdrop-blur-md border-b border-white/5' : 'py-6 bg-transparent'}`}>
      <div className="container mx-auto px-6 flex justify-between items-center">
        <button type="button" onClick={handleLogoClick} className="flex items-center gap-2 group">
          <div className="w-8 h-8 bg-brand rounded-lg flex items-center justify-center group-hover:bg-white transition-colors">
             <svg aria-hidden="true" className="w-5 h-5 text-black" fill="none" stroke="currentColor" viewBox="0 0 24 24"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth="2" d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10"></path></svg>
          </div>
          <span className="font-bold text-xl tracking-tight">Contacts <span className="text-brand">Cleaner</span></span>
        </button>
        
        {currentView === 'home' && (
          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-400">
            <a 
              href="#features" 
              onClick={(e) => handleSectionClick(e, 'features')}
              className="hover:text-white transition-colors"
            >
              Features
            </a>
            <a 
              href="#whatsapp" 
              onClick={(e) => handleSectionClick(e, 'whatsapp')}
              className="hover:text-white transition-colors"
            >
              WhatsApp Intelligence
            </a>
            <a 
              href="#security" 
              onClick={(e) => handleSectionClick(e, 'security')}
              className="hover:text-white transition-colors"
            >
              Privacy
            </a>
          </div>
        )}

        <div className="flex items-center gap-4">
          {currentView !== 'home' && (
             <button
                type="button"
                onClick={() => onNavigate('home')}
                className="text-sm font-medium text-gray-400 hover:text-white transition-colors"
             >
                Back to Home
             </button>
          )}
          <button type="button" className="hidden md:flex px-5 py-2 rounded-full bg-white/10 hover:bg-brand hover:text-black border border-white/10 transition-all duration-300 text-sm font-semibold">
            Get App
          </button>
        </div>
      </div>
    </nav>
  );
};
