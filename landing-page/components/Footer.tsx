
import React from 'react';

type ViewState = 'home' | 'privacy' | 'terms' | 'support';

interface FooterProps {
  onNavigate: (view: ViewState) => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate }) => {
  return (
    <footer className="py-12 bg-black border-t border-white/10 text-sm">
      <div className="container mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-8">
        
        <div className="flex items-center gap-2">
           <div className="w-6 h-6 bg-brand rounded flex items-center justify-center text-black">
             <svg className="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
           </div>
           <span className="font-semibold text-gray-300">Contacts Cleaner</span>
        </div>

        <div className="flex gap-8 text-gray-500">
          <button
            type="button"
            onClick={() => onNavigate('privacy')}
            className="hover:text-brand transition-colors text-left"
          >
            Privacy Policy
          </button>
          <button
            type="button"
            onClick={() => onNavigate('terms')}
            className="hover:text-brand transition-colors text-left"
          >
            Terms of Service
          </button>
          <button
            type="button"
            onClick={() => onNavigate('support')}
            className="hover:text-brand transition-colors text-left"
          >
            Support
          </button>
        </div>

        <div className="text-gray-600">
          © {new Date().getFullYear()} ContactsCleaner.tech
        </div>
      </div>
    </footer>
  );
};
