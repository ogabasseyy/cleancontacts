
import React, { useState, useEffect, useRef } from 'react';
import { APP_STORE_LINK } from '../constants';
import type { ViewState } from '../types';

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
    <nav aria-label="Main navigation" className={`fixed top-0 left-0 right-0 z-50 transition-all duration-300 ${scrolled ? 'py-4 bg-dark/80 backdrop-blur-md border-b border-white/5' : 'py-6 bg-transparent'}`}>
      <div className="container mx-auto px-6 flex justify-between items-center">
        <button type="button" onClick={handleLogoClick} className="flex items-center gap-2 group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded-xl">
          <picture>
            <source srcSet="/logo.webp" type="image/webp" />
            <img src="/logo.png" alt="Contacts Cleaner" width="40" height="40" fetchPriority="high" className="w-10 h-10 rounded-xl" />
          </picture>
          <span className="font-bold text-xl tracking-tight">Contacts <span className="text-brand">Cleaner</span></span>
        </button>
        
        {currentView === 'home' && (
          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-400">
            <a 
              href="#features" 
              onClick={(e) => handleSectionClick(e, 'features')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded"
            >
              Features
            </a>
            <a 
              href="#whatsapp" 
              onClick={(e) => handleSectionClick(e, 'whatsapp')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded"
            >
              WhatsApp Intelligence
            </a>
            <a 
              href="#security" 
              onClick={(e) => handleSectionClick(e, 'security')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded"
            >
              Privacy
            </a>
          </div>
        )}

        <div className="flex items-center gap-4">
          <button
            type="button"
            onClick={() => onNavigate('blog')}
            aria-current={currentView === 'blog' || currentView === 'blog-post' ? 'page' : undefined}
            className={`hidden md:block text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded px-1 py-0.5 ${currentView === 'blog' || currentView === 'blog-post' ? 'text-white' : 'text-gray-400 hover:text-white'}`}
          >
            Blog
          </button>
          {currentView !== 'home' && currentView !== 'blog' && currentView !== 'blog-post' && (
             <button
                type="button"
                onClick={() => onNavigate('home')}
                className="text-sm font-medium text-gray-400 hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand rounded px-1 py-0.5"
             >
                Back to Home
             </button>
          )}
          <a href={APP_STORE_LINK} target="_blank" rel="noopener noreferrer" className="hidden md:flex px-5 py-2 rounded-full bg-white/10 hover:bg-brand hover:text-black border border-white/10 transition-all duration-300 text-sm font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark">
            Get App
          </a>
        </div>
      </div>
    </nav>
  );
};
