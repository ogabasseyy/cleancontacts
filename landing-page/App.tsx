
import React, { Suspense } from 'react';
import { BrowserRouter, Routes, Route, Navigate, useNavigate, useLocation } from 'react-router';
import { Navbar } from './components/Navbar';
import { Hero } from './components/Hero';
import { Features } from './components/Features';
import { WhatsAppSection } from './components/WhatsAppSection';
import { Security } from './components/Security';
import { FAQ } from './components/FAQ';
import { Footer } from './components/Footer';
import BlogErrorBoundary from './components/BlogErrorBoundary';
import type { ViewState } from './types';

const PrivacyCard = React.lazy(() => import('./components/PrivacyCard').then(m => ({ default: m.PrivacyCard })));
const TermsCard = React.lazy(() => import('./components/TermsCard').then(m => ({ default: m.TermsCard })));
const SupportCard = React.lazy(() => import('./components/SupportCard').then(m => ({ default: m.SupportCard })));
const BlogList = React.lazy(() => import('./components/BlogList'));
const BlogPost = React.lazy(() => import('./components/BlogPost'));

const AppContent: React.FC = () => {
  const navigate = useNavigate();
  const location = useLocation();

  const getCurrentView = (): ViewState => {
    const normalizedPath = location.pathname.replace(/\/+$/, '') || '/';
    switch (normalizedPath) {
      case '/privacy': return 'privacy';
      case '/terms': return 'terms';
      case '/support': return 'support';
      case '/blog': return 'blog';
      default:
        if (normalizedPath.startsWith('/blog/')) return 'blog-post';
        return 'home';
    }
  };

  const navigateTo = (view: ViewState) => {
    const path = view === 'home' ? '/' : `/${view}`;
    navigate(path);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-dark text-white selection:bg-brand/30 selection:text-brand relative overflow-x-hidden">
      {/* Skip to content — accessibility */}
      <a
        href="#main-content"
        className="sr-only focus:not-sr-only focus:fixed focus:top-4 focus:left-4 focus:z-[100] focus:px-4 focus:py-2 focus:bg-brand focus:text-black focus:rounded-lg focus:text-sm focus:font-semibold"
      >
        Skip to content
      </a>

      {/* Global Background Glows */}
      <div className="fixed top-0 left-0 w-full h-[800px] bg-[radial-gradient(ellipse_at_top,_rgba(37,211,102,0.15)_0%,_rgba(5,5,5,0)_70%)] pointer-events-none z-0" />

      <div className="relative z-10 flex flex-col min-h-screen">
        <Navbar currentView={getCurrentView()} onNavigate={navigateTo} />

        <main id="main-content" tabIndex={-1} className="flex-grow focus:outline-none">
          <Routes>
            <Route path="/" element={
              <>
                <Hero />
                <Features />
                <WhatsAppSection />
                <Security />
                <FAQ />
              </>
            } />

            <Route path="/privacy" element={
              <Suspense fallback={<div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl" />}>
                <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                  <PrivacyCard />
                </div>
              </Suspense>
            } />

            <Route path="/terms" element={
              <Suspense fallback={<div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl" />}>
                <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                  <TermsCard />
                </div>
              </Suspense>
            } />

            <Route path="/support" element={
              <Suspense fallback={<div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl" />}>
                <div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl">
                  <SupportCard />
                </div>
              </Suspense>
            } />

            <Route path="/blog" element={
              <BlogErrorBoundary>
                <Suspense fallback={<div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl" />}>
                  <BlogList />
                </Suspense>
              </BlogErrorBoundary>
            } />

            <Route path="/blog/:slug" element={
              <BlogErrorBoundary>
                <Suspense fallback={<div className="pt-32 pb-20 container mx-auto px-6 max-w-4xl" />}>
                  <BlogPost />
                </Suspense>
              </BlogErrorBoundary>
            } />

            <Route path="*" element={<Navigate to="/" replace />} />
          </Routes>
        </main>

        <Footer />
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
