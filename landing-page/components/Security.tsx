
import React from 'react';

export const Security: React.FC = () => {
  return (
    <section id="security" className="py-20 border-t border-white/5 bg-[#080808] scroll-mt-32">
      <div className="container mx-auto px-6 text-center">
        <div className="max-w-3xl mx-auto mb-12">
          <div className="w-16 h-16 bg-white/5 rounded-2xl mx-auto flex items-center justify-center mb-6">
             <svg className="w-8 h-8 text-brand" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
          </div>
          <h2 className="text-3xl font-bold mb-4">Your Contacts Stay on Your Device</h2>
          <p className="text-gray-400">
            We believe your address book is personal. All cleaning, merging, and AI detection happens locally on your phone using on-device processing. 
          </p>
        </div>

        <div className="grid md:grid-cols-3 gap-8 text-left">
          <div className="p-6 rounded-2xl bg-white/5 border border-white/5">
            <h4 className="text-lg font-semibold text-white mb-2">Local Processing</h4>
            <p className="text-sm text-gray-400">No contact lists are ever uploaded to our servers. Processing is done entirely on-device using local heuristics.</p>
          </div>
          <div className="p-6 rounded-2xl bg-white/5 border border-white/5">
             <h4 className="text-lg font-semibold text-white mb-2">No Tracking</h4>
             <p className="text-sm text-gray-400">We don't track who you call or message. Our business model is software sales, not data sales.</p>
          </div>
          <div className="p-6 rounded-2xl bg-white/5 border border-white/5">
             <h4 className="text-lg font-semibold text-white mb-2">Undo Protection</h4>
             <p className="text-sm text-gray-400">Accidentally deleted a contact? Our granular undo system restores it instantly.</p>
          </div>
        </div>
      </div>
    </section>
  );
};
