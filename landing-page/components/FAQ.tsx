
import React, { useState } from 'react';

const faqs = [
  {
    question: "How does Contacts Cleaner find duplicate contacts?",
    answer: "Contacts Cleaner uses AI and fuzzy matching with an 82% similarity threshold to analyze names, phone numbers, and email addresses. It detects exact duplicates, near-duplicates, and similar contacts across Google, iCloud, and device accounts — even when they have slight variations or misspellings."
  },
  {
    question: "Is Contacts Cleaner safe for my contacts?",
    answer: "Yes. All contact processing happens locally on your device — your contacts are never uploaded to external servers. The app creates an automatic backup before any changes, and every action can be undone instantly with the built-in undo history."
  },
  {
    question: "Does it work with iCloud and Google Contacts?",
    answer: "Yes. Contacts Cleaner is the only app that merges duplicates across Google, iCloud, and device accounts intelligently. It detects when the same person exists in multiple accounts and merges them without losing any data from either source."
  },
  {
    question: "How is this different from iPhone's built-in duplicate detection?",
    answer: "Apple's built-in detection only catches exact matches. Contacts Cleaner uses AI-powered fuzzy matching to find near-duplicates, detects 12 types of junk contacts, offers WhatsApp intelligence to categorize your address book, and works across multiple account sources simultaneously."
  },
  {
    question: "What types of junk contacts does it detect?",
    answer: "Contacts Cleaner identifies 12 types of junk including missing names, emoji-only contacts, invalid characters, contacts without phone numbers, single-character names, numerical-only names, and entries containing sensitive data like SSN or credit card numbers hidden in contact fields."
  },
  {
    question: "Can I undo changes made by Contacts Cleaner?",
    answer: "Yes. Every action takes an automatic snapshot of your contacts. You can roll back any merge, deletion, or format change instantly using the built-in undo history. Your original contacts are always recoverable — no changes are permanent until you're satisfied."
  }
];

export const FAQ: React.FC = () => {
  const [openIndex, setOpenIndex] = useState<number | null>(null);

  const toggle = (index: number) => {
    setOpenIndex(openIndex === index ? null : index);
  };

  return (
    <section id="faq" className="py-20 bg-dark relative scroll-mt-32" aria-labelledby="faq-heading">
      <div className="container mx-auto px-6 max-w-3xl">
        <div className="mb-12 text-center">
          <h2 id="faq-heading" className="text-3xl md:text-5xl font-bold mb-6">Frequently Asked Questions</h2>
          <p className="text-gray-400">Everything you need to know about cleaning and organizing your contacts.</p>
        </div>

        <div className="space-y-3">
          {faqs.map((faq, index) => (
            <div
              key={index}
              className="glass-panel rounded-2xl overflow-hidden transition-all duration-300"
            >
              <h3 id={`faq-question-${index}`}>
                <button
                  type="button"
                  onClick={() => toggle(index)}
                  className="w-full flex items-center justify-between p-6 text-left gap-4 hover:bg-white/5 focus-visible:outline-none focus-visible:ring-2 focus-visible:ring-brand transition-colors"
                  aria-expanded={openIndex === index}
                  aria-controls={`faq-answer-${index}`}
                >
                  <span className="text-base md:text-lg font-semibold text-white">{faq.question}</span>
                  <svg
                    className={`w-5 h-5 text-brand shrink-0 transition-transform duration-300 ${openIndex === index ? 'rotate-180' : ''}`}
                    fill="none"
                    viewBox="0 0 24 24"
                    stroke="currentColor"
                    aria-hidden="true"
                    focusable="false"
                  >
                    <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
                  </svg>
                </button>
              </h3>
              <div
                id={`faq-answer-${index}`}
                role="region"
                aria-labelledby={`faq-question-${index}`}
                className={`overflow-hidden transition-all duration-300 ${openIndex === index ? 'max-h-96 opacity-100' : 'max-h-0 opacity-0'}`}
              >
                <p className="px-6 pb-6 text-gray-400 leading-relaxed">{faq.answer}</p>
              </div>
            </div>
          ))}
        </div>
      </div>
    </section>
  );
};
