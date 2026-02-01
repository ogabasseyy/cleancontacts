
import React from 'react';

export const PrivacyCard: React.FC = () => {
  return (
    <div className="glass-panel backdrop-blur-xl rounded-3xl p-8 md:p-12 shadow-2xl animate-in fade-in zoom-in duration-300">
      <div className="prose prose-invert max-w-none prose-headings:font-bold prose-p:text-gray-400 prose-strong:text-brand prose-a:text-brand">
        
        <div className="flex items-center gap-2 mb-8">
           <span className="h-1 w-12 bg-brand rounded-full"></span>
           <span className="text-xs font-mono text-brand tracking-widest uppercase">Effective Date: October 2023</span>
        </div>

        <h1 className="text-4xl md:text-5xl font-extrabold mb-8 bg-clip-text text-transparent bg-gradient-to-b from-white to-gray-500">
          Privacy Policy
        </h1>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-3">
            1. Introduction
          </h2>
          <p className="leading-relaxed mb-4">
            Welcome to <strong className="text-white">Contacts Cleaner</strong>. Your privacy is paramount to our mission. This Privacy Policy describes how your personal information is collected, used, and shared when you install or use our mobile application.
          </p>
          <p className="leading-relaxed">
            By using Contacts Cleaner, you agree to the collection and use of information in accordance with this policy. We prioritize local processing to ensure your sensitive data stays on your device whenever possible.
          </p>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-3">
            2. Information We Collect
          </h2>
          <div className="grid md:grid-cols-2 gap-6 not-prose">
            <div className="bg-black/40 border border-white/5 p-5 rounded-2xl">
              <h3 className="text-brand font-semibold mb-3">Contact Data</h3>
              <p className="text-sm text-gray-400">
                We request permission to access your device's contact list to identify duplicates, incomplete entries, and optimize your address book.
              </p>
            </div>
            <div className="bg-black/40 border border-white/5 p-5 rounded-2xl">
              <h3 className="text-brand font-semibold mb-3">Device Information</h3>
              <p className="text-sm text-gray-400">
                Technical data such as device model, OS version, and unique device identifiers may be collected to improve app performance.
              </p>
            </div>
          </div>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-3">
            3. How We Use Data
          </h2>
          <ul className="space-y-4 list-none p-0 not-prose">
            {[
              "Identifying and merging duplicate contacts.",
              "Removing empty or invalid contact entries.",
              "Formatting phone numbers for global consistency.",
              "Backing up contacts (if explicitly opted-in by the user).",
              "Improving our AI algorithms for better contact recognition."
            ].map((item, idx) => (
              <li key={idx} className="flex items-start gap-3 text-gray-400">
                <svg className="h-5 w-5 text-brand mt-0.5 shrink-0" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2.5} d="M5 13l4 4L19 7" />
                </svg>
                <span>{item}</span>
              </li>
            ))}
          </ul>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-3">
            4. Data Security & Storage
          </h2>
          <p className="leading-relaxed">
            We implement industry-standard security measures. Your contact data is <strong className="text-white">processed locally</strong> on your device using on-device machine learning. We do not upload your full contact list to our servers unless you initiate a cloud backup feature.
          </p>
        </section>

        <section className="mb-12">
          <h2 className="text-2xl font-bold text-white mb-4 flex items-center gap-3">
            5. Your Rights
          </h2>
          <p className="leading-relaxed mb-6">
            Under GDPR and CCPA, you have the right to:
          </p>
          <div className="space-y-3 not-prose">
            <div className="flex gap-4 p-4 rounded-xl border border-white/5 hover:border-brand/50 transition-colors bg-white/5">
              <span className="text-brand font-mono font-bold">01</span>
              <div>
                <h4 className="text-white font-medium">Access & Portability</h4>
                <p className="text-xs text-gray-400">Request a copy of the data we process about you.</p>
              </div>
            </div>
            <div className="flex gap-4 p-4 rounded-xl border border-white/5 hover:border-brand/50 transition-colors bg-white/5">
              <span className="text-brand font-mono font-bold">02</span>
              <div>
                <h4 className="text-white font-medium">Erasure</h4>
                <p className="text-xs text-gray-400">Request the deletion of your account and associated data.</p>
              </div>
            </div>
            <div className="flex gap-4 p-4 rounded-xl border border-white/5 hover:border-brand/50 transition-colors bg-white/5">
              <span className="text-brand font-mono font-bold">03</span>
              <div>
                <h4 className="text-white font-medium">Opt-Out</h4>
                <p className="text-xs text-gray-400">Revoke contact permissions at any time via device settings.</p>
              </div>
            </div>
          </div>
        </section>

        <section className="mt-16 pt-12 border-t border-white/10">
          <h2 className="text-2xl font-bold text-white mb-6">Contact Us</h2>
          <p className="mb-6">
            If you have any questions about this Privacy Policy, please contact our Cyber-Security team at:
          </p>
          <a 
            href="mailto:privacy@contactscleaner.tech" 
            className="inline-flex items-center gap-3 px-6 py-3 rounded-full bg-brand/10 border border-brand/30 text-brand hover:bg-brand/20 transition-all font-semibold no-underline"
          >
            <svg className="h-5 w-5" fill="none" viewBox="0 0 24 24" stroke="currentColor">
              <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 8l7.89 5.26a2 2 0 002.22 0L21 8M5 19h14a2 2 0 002-2V7a2 2 0 00-2-2H5a2 2 0 00-2 2v10a2 2 0 002 2z" />
            </svg>
            privacy@contactscleaner.tech
          </a>
        </section>
      </div>
    </div>
  );
};
