
import React from 'react';

export const TermsCard: React.FC = () => {
  return (
    <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl animate-in fade-in zoom-in duration-300">
      <div className="prose prose-invert max-w-none prose-headings:font-bold prose-p:text-gray-400 prose-strong:text-brand prose-a:text-brand">
        
        <div className="flex items-center gap-2 mb-8">
           <span className="h-1 w-12 bg-brand rounded-full"></span>
           <span className="text-xs font-mono text-brand tracking-widest uppercase">Last Updated: January 2025</span>
        </div>

        <h1 className="text-4xl md:text-5xl font-extrabold mb-8 bg-clip-text text-transparent bg-gradient-to-b from-white to-gray-500">
          Terms of Service
        </h1>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4">1. Acceptance of Terms</h2>
          <p className="leading-relaxed mb-4">
            By accessing and using <strong className="text-white">Contacts Cleaner</strong>, you accept and agree to be bound by the terms and provision of this agreement. In addition, when using this app's particular services, you shall be subject to any posted guidelines or rules applicable to such services.
          </p>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4">2. License to Use</h2>
          <p className="leading-relaxed mb-4">
             Contacts Cleaner grants you a revocable, non-exclusive, non-transferable, limited license to download, install and use the Application strictly in accordance with the terms of this Agreement.
          </p>
          <ul className="list-disc pl-5 text-gray-400 space-y-2">
             <li>You may not decompile, reverse engineer, disassemble, attempt to derive the source code of, or decrypt the Application.</li>
             <li>You may not use the Application for any revenue generating endeavor, commercial enterprise, or other purpose for which it is not designed or intended.</li>
          </ul>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4">3. User Data & Privacy</h2>
          <p className="leading-relaxed">
            We respect your privacy. As detailed in our Privacy Policy, your contact data is processed locally on your device. We do not sell your data. By using our service, you agree to the data practices described in our Privacy Policy.
          </p>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4">4. Subscriptions & Billing</h2>
          <p className="leading-relaxed">
            Some features of the Service may be billed on a subscription basis. You will be billed in advance on a recurring and periodic basis ("Billing Cycle"). Billing cycles are set either on a monthly or annual basis, depending on the type of subscription plan you select when purchasing a Subscription.
          </p>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4">5. Limitation of Liability</h2>
          <p className="leading-relaxed">
            In no event shall Contacts Cleaner, nor its directors, employees, partners, agents, suppliers, or affiliates, be liable for any indirect, incidental, special, consequential or punitive damages, including without limitation, loss of profits, data, use, goodwill, or other intangible losses, resulting from your access to or use of or inability to access or use the Service.
          </p>
        </section>

        <section className="mt-16 pt-12 border-t border-white/10">
          <h2 className="text-2xl font-bold text-white mb-6">Contact Us</h2>
          <p className="mb-6">
            If you have any questions about these Terms, please contact us.
          </p>
          <a 
            href="mailto:legal@contactscleaner.tech" 
            className="inline-flex items-center gap-3 px-6 py-3 rounded-full bg-white/5 border border-white/10 text-white hover:bg-white/10 transition-all font-semibold no-underline"
          >
            legal@contactscleaner.tech
          </a>
        </section>
      </div>
    </div>
  );
};
