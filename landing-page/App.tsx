
import React from 'react';
import { BrowserRouter, Routes, Route, useNavigate, useLocation } from 'react-router-dom';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { Features } from './components/Features';
import { WhatsAppSection } from './components/WhatsAppSection';
import { Security } from './components/Security';
import { Footer } from './components/Footer';
import { PrivacyCard } from './components/PrivacyCard';
import { TermsCard } from './components/TermsCard';
import { SupportCard } from './components/SupportCard';

type ViewState = 'home' | 'privacy' | 'terms' | 'support';

const AppContent: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const getCurrentView = (): ViewState => {
    const normalizedPath = location.pathname.replace(/\/+$/, '') || '/';
    switch (normalizedPath) {
      case '/privacy': return 'privacy';
      case '/terms': return 'terms';
      case '/support': return 'support';
      default: return 'home';
    }
  };

  const navigateTo = (view: ViewState) => {
    const path = view === 'home' ? '/' : `/${view}`;
    navigate(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-dark text-white selection:bg-brand/30 selection:text-brand relative overflow-x-hidden">
      {/* Global Background Glows */}
      <div className="fixed top-0 left-0 w-full h-[800px] bg-[radial-gradient(ellipse_at_top,_rgba(37,211,102,0.15)_0%,_rgba(5,5,5,0)_70%)] pointer-events-none z-0" />

      <div className="relative z-10 flex flex-col min-h-screen">
        <Navbar currentView={getCurrentView()} onNavigate={navigateTo} />

        <main className="flex-grow">
          <Routes>
            <Route path="/" element={
              <>
                <Hero />
                <Features />
                <WhatsAppSection />
                <Security />
              </>
            } />

            <Route path="/privacy" element={
              <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                <PrivacyCard />
              </div>
            } />

            <Route path="/terms" element={
              <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                <TermsCard />
              </div>
            } />

            <Route path="/support" element={
              <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                <SupportCard />
              </div>
            } />
          </Routes>
        </main>

        <Footer onNavigate={navigateTo} />
      </div>
    </div>
  );
};

const App: React.FC = () => {
  return (
    <BrowserRouter>
      <AppContent />
    </BrowserRouter>
  );
};

export default App;
