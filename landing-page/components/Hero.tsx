import React from 'react';
import { PhoneMockup } from './PhoneMockup';
import { APP_STORE_LINK, PLAY_STORE_LINK } from '../constants';

export const Hero: React.FC = () => {
   return (
      <section className="relative pt-24 pb-20 md:pt-36 md:pb-32 overflow-hidden">
         <div className="container mx-auto px-6">
            <div className="flex flex-col lg:flex-row items-center gap-16 lg:gap-24">

               {/* Text Content */}
               <div className="flex-1 text-center lg:text-left z-10">
                  <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand/10 border border-brand/20 text-brand text-xs font-semibold mb-6 uppercase tracking-wider">
                     <span className="w-2 h-2 rounded-full bg-brand animate-pulse" aria-hidden="true"></span>
                     v2.0 Now Available
                  </div>

                  <h1 className="text-5xl md:text-7xl font-bold tracking-tight leading-[1.1] mb-6">
                     Clean Your Contacts. <br />
                     <span className="text-gradient-green">Instantly.</span>
                  </h1>

                  <p className="text-lg text-gray-400 mb-8 max-w-xl mx-auto lg:mx-0 leading-relaxed">
                     The AI-powered contact manager that detects junk, merges cross-platform duplicates, and organizes your WhatsApp connections.
                  </p>

                  <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start items-center">
                     <a href={APP_STORE_LINK} target="_blank" rel="noopener noreferrer" className="flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl bg-white text-black hover:bg-gray-100 transition-colors font-semibold focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark">
                        <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.68-.83 1.14-1.99 1.01-3.15-1.02.04-2.26.68-3 1.54-.78.89-1.46 2.33-1.28 3.51 1.12.09 2.29-.57 3.27-1.9z" /></svg>
                        <span>App Store <span className="sr-only">(opens in a new tab)</span></span>
                     </a>
                     <a href={PLAY_STORE_LINK} target="_blank" rel="noopener noreferrer" className="flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl glass-panel hover:bg-white/10 transition-colors font-semibold border border-white/10 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand focus-visible:ring-offset-2 focus-visible:ring-offset-dark">
                        <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true" focusable="false"><path d="M3.609 1.814L13.792 12 3.61 22.186a.996.996 0 0 1-.61-.92V2.734a1 1 0 0 1 .609-.92zm10.89 10.893l2.302 2.302-10.937 6.333 8.635-8.635zm3.199-3.198l2.807 1.626a1 1 0 0 1 0 1.73l-2.808 1.626L15.206 12l2.492-2.491zM5.864 2.658L16.802 8.99l-2.303 2.303-8.635-8.635z" /></svg>
                        <span>Google Play <span className="sr-only">(opens in a new tab)</span></span>
                     </a>
                  </div>

                  <div className="mt-8 flex items-center justify-center lg:justify-start gap-4 text-sm text-gray-500">
                     <div className="flex -space-x-2" aria-hidden="true" focusable="false">
                        {[1, 2, 3, 4].map(i => (
                           <div key={i} className="w-8 h-8 rounded-full border border-dark bg-gray-800"></div>
                        ))}
                     </div>
                     <p>Trusted by 10,000+ users</p>
                  </div>
               </div>

               {/* Graphical Representation (Phone) */}
               <PhoneMockup />

            </div>
         </div>
      </section>
   );
};
