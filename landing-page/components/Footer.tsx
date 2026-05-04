
import React from 'react';
import { APP_STORE_LINK, PLAY_STORE_LINK } from '../constants';
import type { ViewState } from '../types';

interface FooterProps {
  onNavigate: (view: ViewState) => void;
}

export const Footer: React.FC<FooterProps> = ({ onNavigate }) => {
  return (
    <footer className="py-12 bg-black border-t border-white/10 text-sm">
      <div className="container mx-auto px-6 flex flex-col md:flex-row justify-between items-center gap-8">

        <div className="flex items-center gap-2">
          <picture>
            <source srcSet="/logo.webp" type="image/webp" />
            <img src="/logo.png" alt="" aria-hidden="true" width="32" height="32" loading="lazy" className="w-8 h-8 rounded-lg" />
          </picture>
          <span className="font-semibold text-gray-300">ContactsCleaner</span>
        </div>

        <div className="flex flex-col items-center md:items-end gap-4">
          <div className="flex gap-6 text-gray-400 mb-2">
            <a
              href={APP_STORE_LINK}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-brand transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              <span className="sr-only">(opens in a new tab)</span>
              App Store
            </a>
            <a
              href={PLAY_STORE_LINK}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-brand transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              <span className="sr-only">(opens in a new tab)</span>
              Google Play
            </a>
          </div>
          <div className="flex gap-8 text-gray-400">
            <button
              type="button"
              onClick={() => onNavigate('blog')}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Blog
            </button>
            <button
              type="button"
              onClick={() => onNavigate('privacy')}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Privacy
            </button>
            <button
              type="button"
              onClick={() => onNavigate('terms')}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Terms
            </button>
            <button
              type="button"
              onClick={() => onNavigate('support')}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Support
            </button>
          </div>
        </div>

        <div className="text-gray-600">
          © {new Date().getFullYear()} ContactsCleaner.tech
        </div>
      </div>
    </footer>
  );
};
