
import React from 'react';
import { Link } from 'react-router-dom';
import { APP_STORE_LINK, PLAY_STORE_LINK } from '../constants';

export const Footer: React.FC = () => {
  const handleRouteClick = (event: React.MouseEvent<HTMLAnchorElement>) => {
    if (
      event.button === 0 &&
      !event.metaKey &&
      !event.ctrlKey &&
      !event.shiftKey &&
      !event.altKey
    ) {
      window.scrollTo({ top: 0, behavior: 'smooth' });
    }
  };

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
              App Store
              <span className="sr-only"> (opens in a new tab)</span>
            </a>
            <a
              href={PLAY_STORE_LINK}
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-brand transition-colors focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Google Play
              <span className="sr-only"> (opens in a new tab)</span>
            </a>
          </div>
          <div className="flex gap-8 text-gray-400">
            <Link
              to="/blog"
              onClick={handleRouteClick}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Blog
            </Link>
            <Link
              to="/privacy"
              onClick={handleRouteClick}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Privacy
            </Link>
            <Link
              to="/terms"
              onClick={handleRouteClick}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Terms
            </Link>
            <Link
              to="/support"
              onClick={handleRouteClick}
              className="hover:text-brand transition-colors text-left focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-black rounded-sm"
            >
              Support
            </Link>
          </div>
        </div>

        <div className="text-gray-600">
          © {new Date().getFullYear()} ContactsCleaner.tech
        </div>
      </div>
    </footer>
  );
};
