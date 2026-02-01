
import React from 'react';

export const WhatsAppSection: React.FC = () => {
  return (
    <section id="whatsapp" className="py-24 relative overflow-hidden scroll-mt-32">
      {/* Background Gradient */}
      <div className="absolute inset-0 bg-gradient-to-b from-[#0B120E] to-dark z-0"></div>
      
      <div className="container mx-auto px-6 relative z-10">
        <div className="glass-panel rounded-3xl p-8 md:p-16 border border-brand/20 bg-gradient-to-br from-[#0a1a10] to-[#050505]">
          <div className="flex flex-col lg:flex-row items-center gap-12">
            
            <div className="flex-1 space-y-8">
              <div className="inline-block px-4 py-1 rounded-full bg-brand/10 text-brand text-xs font-bold uppercase tracking-widest">
                Exclusive Feature
              </div>
              <h2 className="text-3xl md:text-5xl font-bold">
                WhatsApp <span className="text-brand">Intelligence</span>
              </h2>
              <p className="text-gray-400 text-lg">
                Stop guessing who has WhatsApp. Contacts Cleaner automatically categorizes your address book based on WhatsApp availability.
              </p>
              
              <ul className="space-y-4">
                {[
                  "Identify contacts with WhatsApp installed",
                  "Distinguish Personal vs Business accounts",
                  "Filter non-WhatsApp users for SMS campaigns",
                  "Privacy-first real-time checking"
                ].map((item, idx) => (
                  <li key={idx} className="flex items-center gap-3">
                    <div className="w-6 h-6 rounded-full bg-brand/20 flex items-center justify-center text-brand shrink-0">
                      <svg className="w-3.5 h-3.5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={3} d="M5 13l4 4L19 7" /></svg>
                    </div>
                    <span className="text-gray-300 font-medium">{item}</span>
                  </li>
                ))}
              </ul>
            </div>

            <div className="flex-1 w-full max-w-sm">
              <div className="relative bg-black/40 p-6 rounded-2xl border border-white/5 backdrop-blur-xl">
                 <div className="flex items-center justify-between mb-6">
                    <span className="text-gray-500 text-sm font-semibold uppercase">Scan Results</span>
                    <span className="text-brand text-xs bg-brand/10 px-2 py-1 rounded">Active</span>
                 </div>
                 
                 <div className="space-y-3">
                    <div className="flex items-center justify-between p-3 rounded-lg bg-white/5 border border-white/5">
                       <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-green-900/50 flex items-center justify-center text-brand text-xs">WA</div>
                          <span className="text-sm">WhatsApp Personal</span>
                       </div>
                       <span className="font-mono text-brand">432</span>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-white/5 border border-white/5">
                       <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-blue-900/50 flex items-center justify-center text-blue-400 text-xs">WB</div>
                          <span className="text-sm">WhatsApp Business</span>
                       </div>
                       <span className="font-mono text-blue-400">85</span>
                    </div>
                    <div className="flex items-center justify-between p-3 rounded-lg bg-white/5 border border-white/5 opacity-50">
                       <div className="flex items-center gap-3">
                          <div className="w-8 h-8 rounded-full bg-gray-700 flex items-center justify-center text-gray-400 text-xs">NA</div>
                          <span className="text-sm">Not on WhatsApp</span>
                       </div>
                       <span className="font-mono text-gray-400">12</span>
                    </div>
                 </div>
              </div>
            </div>

          </div>
        </div>
      </div>
    </section>
  );
};
