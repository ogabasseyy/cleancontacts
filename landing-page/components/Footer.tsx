
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
          <img src="/logo.png" alt="CleanContacts" className="w-8 h-8 rounded-lg" />
          <span className="font-semibold text-gray-300">Contacts Cleaner</span>
        </div>

        <div className="flex flex-col items-center md:items-end gap-4">
          <div className="flex gap-6 text-gray-500 mb-2">
            <a
              href="https://apps.apple.com/app/id6758563652"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-brand transition-colors"
            >
              App Store
            </a>
            <a
              href="https://play.google.com/store/apps/details?id=com.ogabassey.contactscleaner"
              target="_blank"
              rel="noopener noreferrer"
              className="hover:text-brand transition-colors"
            >
              Google Play
            </a>
          </div>
          <div className="flex gap-8 text-gray-500">
            <button
              type="button"
              onClick={() => onNavigate('privacy')}
              className="hover:text-brand transition-colors text-left"
            >
              Privacy
            </button>
            <button
              type="button"
              onClick={() => onNavigate('terms')}
              className="hover:text-brand transition-colors text-left"
            >
              Terms
            </button>
            <button
              type="button"
              onClick={() => onNavigate('support')}
              className="hover:text-brand transition-colors text-left"
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
