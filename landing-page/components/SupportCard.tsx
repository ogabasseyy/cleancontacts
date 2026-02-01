
import React from 'react';

export const SupportCard: React.FC = () => {
  return (
    <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl animate-in fade-in zoom-in duration-300">
       <div className="text-center mb-12">
          <div className="inline-flex items-center justify-center w-16 h-16 rounded-2xl bg-brand/20 text-brand mb-6">
             <svg className="w-8 h-8" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M18.364 5.636l-3.536 3.536m0 5.656l3.536 3.536M9.172 9.172L5.636 5.636m3.536 9.192l-3.536 3.536M21 12a9 9 0 11-18 0 9 9 0 0118 0zm-5 0a4 4 0 11-8 0 4 4 0 018 0z" /></svg>
          </div>
          <h1 className="text-4xl md:text-5xl font-extrabold mb-4 bg-clip-text text-transparent bg-gradient-to-b from-white to-gray-500">
            How can we help?
          </h1>
          <p className="text-xl text-gray-400">Search our knowledge base or get in touch.</p>
       </div>

       <div className="grid gap-6 mb-12">
          <div className="p-6 rounded-2xl bg-white/5 border border-white/5 hover:border-brand/30 transition-colors">
             <h3 className="text-lg font-bold text-white mb-2">How does the duplicate detection work?</h3>
             <p className="text-gray-400 leading-relaxed">Our AI uses a fuzzy matching algorithm that looks at names, phone numbers, and emails. It assigns a similarity score to potential matches. Pairs with a score above 82% are flagged as duplicates for your review.</p>
          </div>
          
          <div className="p-6 rounded-2xl bg-white/5 border border-white/5 hover:border-brand/30 transition-colors">
             <h3 className="text-lg font-bold text-white mb-2">Is my data safe?</h3>
             <p className="text-gray-400 leading-relaxed">Absolutely. Contacts Cleaner processes your contacts locally on your device. We do not upload your address book to any external servers for processing.</p>
          </div>

          <div className="p-6 rounded-2xl bg-white/5 border border-white/5 hover:border-brand/30 transition-colors">
             <h3 className="text-lg font-bold text-white mb-2">Can I undo changes?</h3>
             <p className="text-gray-400 leading-relaxed">Yes! Every merge or delete action creates a snapshot. You can go to the "History" tab in the app to roll back any changes made in the last 30 days.</p>
          </div>
       </div>

       <div className="p-8 rounded-2xl bg-brand/5 border border-brand/10 text-center">
          <h3 className="text-2xl font-bold text-white mb-4">Still need help?</h3>
          <p className="text-gray-400 mb-8 max-w-lg mx-auto">Our support team is available Monday through Friday to assist with any issues you might encounter.</p>
          
          <a 
            href="mailto:support@contactscleaner.tech" 
            className="inline-flex items-center gap-2 px-8 py-4 rounded-full bg-brand text-black hover:bg-white transition-colors font-bold text-lg shadow-lg shadow-brand/20"
          >
            <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" /></svg>
            Contact Support
          </a>
       </div>
    </div>
  );
};
