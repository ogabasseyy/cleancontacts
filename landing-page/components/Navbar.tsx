
import React, { useState, useEffect, useRef } from 'react';
import { Link } from 'react-router';
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

  const isUnmodifiedPrimaryClick = (event: React.MouseEvent<HTMLAnchorElement>) =>
    event.button === 0 &&
    !event.metaKey &&
    !event.ctrlKey &&
    !event.shiftKey &&
    !event.altKey;

  const handleRouteClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    if (isUnmodifiedPrimaryClick(event)) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

  const handleSectionClick = (e: React.MouseEvent<HTMLAnchorElement>, id: string) => {
    if (!isUnmodifiedPrimaryClick(e)) return;
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
        <Link to="/" onClick={handleRouteClick} className="flex items-center gap-2 group focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-xl">
          <picture>
            <source srcSet="/logo.webp" type="image/webp" />
            <img src="/logo.png" alt="" aria-hidden="true" width="40" height="40" fetchPriority="high" className="w-10 h-10 rounded-xl" />
          </picture>
          <span className="font-bold text-xl tracking-tight">Contacts <span className="text-brand">Cleaner</span></span>
        </Link>
        
        {currentView === 'home' && (
          <div className="hidden md:flex items-center gap-8 text-sm font-medium text-gray-400">
            <a 
              href="#features" 
              onClick={(e) => handleSectionClick(e, 'features')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm"
            >
              Features
            </a>
            <a 
              href="#whatsapp" 
              onClick={(e) => handleSectionClick(e, 'whatsapp')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm"
            >
              WhatsApp Intelligence
            </a>
            <a 
              href="#security" 
              onClick={(e) => handleSectionClick(e, 'security')}
              className="hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm"
            >
              Privacy
            </a>
          </div>
        )}

        <div className="flex items-center gap-4">
          <Link
            to="/blog"
            onClick={handleRouteClick}
            aria-current={currentView === 'blog' ? 'page' : undefined}
            className={`hidden md:block text-sm font-medium transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm ${currentView === 'blog' || currentView === 'blog-post' ? 'text-white' : 'text-gray-400 hover:text-white'}`}
          >
            Blog
          </Link>
          {currentView !== 'home' && currentView !== 'blog' && currentView !== 'blog-post' && (
             <Link
                to="/"
                onClick={handleRouteClick}
                className="text-sm font-medium text-gray-400 hover:text-white transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark rounded-sm"
             >
                Back to Home
             </Link>
          )}
          <a href={APP_STORE_LINK} target="_blank" rel="noopener noreferrer" className="hidden md:flex px-5 py-2 rounded-full bg-white/10 hover:bg-brand hover:text-black border border-white/10 transition-all duration-300 active:scale-95 text-sm font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark">
            Get App
            <span className="sr-only"> (opens in a new tab)</span>
          </a>
        </div>
      </div>
    </nav>
  );
};
