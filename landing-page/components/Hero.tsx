
import React from 'react';

export const Hero: React.FC = () => {
  return (
    <section className="relative pt-24 pb-20 md:pt-36 md:pb-32 overflow-hidden">
      <div className="container mx-auto px-6">
        <div className="flex flex-col lg:flex-row items-center gap-16 lg:gap-24">
          
          {/* Text Content */}
          <div className="flex-1 text-center lg:text-left z-10">
            <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-brand/10 border border-brand/20 text-brand text-xs font-semibold mb-6 uppercase tracking-wider">
              <span className="w-2 h-2 rounded-full bg-brand animate-pulse"></span>
              v2.0 Now Available
            </div>
            
            <h1 className="text-5xl md:text-7xl font-bold tracking-tight leading-[1.1] mb-6">
              Clean Your Contacts. <br/>
              <span className="text-gradient-green">Instantly.</span>
            </h1>
            
            <p className="text-lg text-gray-400 mb-8 max-w-xl mx-auto lg:mx-0 leading-relaxed">
              The AI-powered contact manager that detects junk, merges cross-platform duplicates, and organizes your WhatsApp connections.
            </p>
            
            <div className="flex flex-col sm:flex-row gap-4 justify-center lg:justify-start">
              <button type="button" className="flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl bg-white text-black hover:bg-gray-100 transition-colors font-semibold">
                <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M18.71 19.5c-.83 1.24-1.71 2.45-3.05 2.47-1.34.03-1.77-.79-3.29-.79-1.53 0-2 .77-3.27.82-1.31.05-2.3-1.32-3.14-2.53C4.25 17 2.94 12.45 4.7 9.39c.87-1.52 2.43-2.48 4.12-2.51 1.28-.02 2.5.87 3.29.87.78 0 2.26-1.07 3.81-.91.65.03 2.47.26 3.64 1.98-.09.06-2.17 1.28-2.15 3.81.03 3.02 2.65 4.03 2.68 4.04-.03.07-.42 1.44-1.38 2.83M13 3.5c.68-.83 1.14-1.99 1.01-3.15-1.02.04-2.26.68-3 1.54-.78.89-1.46 2.33-1.28 3.51 1.12.09 2.29-.57 3.27-1.9z"/></svg>
                <span>App Store</span>
              </button>
              <button type="button" className="flex items-center justify-center gap-3 px-6 py-3.5 rounded-xl glass-panel hover:bg-white/10 transition-colors font-semibold border-white/10">
                <svg className="w-6 h-6" viewBox="0 0 24 24" fill="currentColor" aria-hidden="true"><path d="M3,20.5V3.5C3,2.91,3.34,2.39,3.84,2.14L13.77,12L3.84,21.86C3.34,21.61,3,21.09,3,20.5M14.47,12L4.54,2.07L18.77,10.18C19.53,10.62,19.53,11.38,18.77,11.82L4.54,19.93L14.47,12M21.66,10.18L15.18,6.47L15.18,17.53L21.66,13.82C22.42,13.38,22.42,12.62,21.66,10.18Z"/></svg>
                <span>Google Play</span>
              </button>
            </div>

            <div className="mt-8 flex items-center justify-center lg:justify-start gap-4 text-sm text-gray-500">
              <div className="flex -space-x-2">
                {[1,2,3,4].map(i => (
                  <div key={i} className="w-8 h-8 rounded-full border border-dark bg-gray-800"></div>
                ))}
              </div>
              <p>Trusted by 10,000+ users</p>
            </div>
          </div>

          {/* Graphical Representation (Phone) */}
          <div className="flex-1 relative z-10 w-full max-w-sm lg:max-w-md mx-auto animate-float">
             {/* Abstract Phone Body */}
             <div className="relative rounded-[2.5rem] bg-gray-900 border-4 border-gray-800 shadow-2xl overflow-hidden aspect-[9/19]">
                {/* Screen Content */}
                <div className="absolute inset-0 bg-black flex flex-col font-sans">
                  {/* Status Bar */}
                  <div className="px-6 pt-4 pb-2 flex justify-between items-center text-xs font-medium text-white/60 z-20">
                    <span>00:04</span>
                    <div className="flex gap-1.5 items-center">
                       <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z"/></svg>
                       <div className="w-5 h-2.5 rounded-[1px] border border-white/40 relative">
                          <div className="absolute inset-0.5 bg-white/40 w-[40%]"></div>
                       </div>
                    </div>
                  </div>

                  {/* App Header */}
                  <div className="px-5 py-3 flex items-center justify-between z-20">
                    <div className="flex items-center gap-4">
                       <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
                       <h1 className="text-xl font-bold text-white">Scan Results</h1>
                    </div>
                    <svg className="w-6 h-6 text-brand" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                  </div>

                  {/* Content Scroll Area */}
                  <div className="flex-1 overflow-y-auto relative z-10 px-4 space-y-4 pt-2 no-scrollbar pb-6">
                     {/* Summary Card */}
                     <div className="bg-[#1c1c1e] rounded-2xl p-5 border border-white/5 shadow-lg">
                        <div className="flex items-start gap-4 mb-6">
                           <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center shrink-0">
                              <svg className="w-6 h-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
                           </div>
                           <div>
                              <div className="text-4xl font-bold text-white tracking-tight">72,858</div>
                              <div className="text-gray-400 text-sm">contacts found</div>
                           </div>
                        </div>
                        <div className="flex border-t border-white/10 pt-4">
                           <div className="flex-1 text-center border-r border-white/10">
                              <div className="text-brand text-xl font-bold">1</div>
                              <div className="text-[10px] text-gray-400 font-medium">Accounts</div>
                           </div>
                           <div className="flex-1 text-center">
                              <div className="text-red-500 text-xl font-bold">47,814</div>
                              <div className="text-[10px] text-gray-400 font-medium">Total Issues</div>
                           </div>
                        </div>
                     </div>

                     {/* High Priority Section */}
                     <div>
                        <h3 className="text-cyan-400 text-xs font-bold uppercase tracking-widest mb-3 ml-1">High Priority</h3>
                        <div className="space-y-3">
                           {/* Format Issues */}
                           <div className="bg-[#111827] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-500">
                              <div className="w-10 h-10 rounded-full bg-cyan-500/10 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Format Issues</h4>
                                 <p className="text-gray-500 text-xs truncate">Fixing Format issues helps identify...</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-cyan-400 font-bold text-lg">13,739</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>

                           {/* Sensitive Data */}
                           <div className="bg-[#1c1917] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-700">
                              <div className="w-10 h-10 rounded-full bg-yellow-500/10 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Sensitive Data</h4>
                                 <p className="text-gray-500 text-xs truncate">Safety Check: Potential sensitive IDs...</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-yellow-500 font-bold text-lg">20</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>
                        </div>
                     </div>

                     {/* Issues Found Section */}
                     <div>
                        <h3 className="text-gray-500 text-xs font-bold uppercase tracking-widest mb-3 ml-1">Issues Found</h3>
                        <div className="space-y-3">
                           {/* Junk Contacts */}
                           <div className="bg-[#1f1212] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-1000">
                              <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Junk Contacts</h4>
                                 <p className="text-gray-500 text-xs truncate">Contacts with missing or invalid data</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-red-500 font-bold text-lg">2,501</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>

                           {/* Missing Numbers */}
                           <div className="bg-[#1f1212] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-1000 delay-100">
                              <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Missing Numbers</h4>
                                 <p className="text-gray-500 text-xs truncate">Contacts without any phone numbers</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-red-500 font-bold text-lg">24</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>

                           {/* Duplicates */}
                           <div className="bg-[#1c1c1e] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-1000 delay-200">
                              <div className="w-10 h-10 rounded-full bg-yellow-600/10 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Duplicates</h4>
                                 <p className="text-gray-500 text-xs truncate">Contacts that appear multiple times</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-yellow-500 font-bold text-lg">31,375</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>

                           {/* Numerical Names */}
                           <div className="bg-[#1f1212] rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-1000 delay-300">
                              <div className="w-10 h-10 rounded-full bg-red-500/20 flex items-center justify-center shrink-0">
                                 <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h7" /></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-semibold text-sm">Numerical Names</h4>
                                 <p className="text-gray-500 text-xs truncate">Names consisting only of digits</p>
                              </div>
                              <div className="text-right shrink-0">
                                 <span className="text-red-500 font-bold text-lg">991</span>
                              </div>
                              <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                           </div>
                        </div>
                     </div>

                     {/* Contact Breakdown */}
                     <div>
                        <h3 className="text-gray-500 text-xs font-bold uppercase tracking-widest mb-3 ml-1">Contact Breakdown</h3>
                        <div className="bg-[#1c1c1e] rounded-xl p-4 border border-white/5 animate-in slide-in-from-bottom-4 duration-1000 delay-500 relative overflow-hidden group">
                           {/* subtle gradient background for whatsapp */}
                           <div className="absolute inset-0 bg-brand/5 group-hover:bg-brand/10 transition-colors"></div>
                           <div className="flex items-start gap-4 relative z-10">
                              <div className="w-10 h-10 rounded-full bg-brand flex items-center justify-center shrink-0 shadow-lg shadow-brand/20">
                                 <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true"><path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.711 2.592 2.654-.696c1.001.572 2.073.811 3.195.811 3.18 0 5.767-2.587 5.767-5.766.001-3.185-2.58-5.794-6.156-5.794zm5.292 6.808c-.219-.109-1.296-.657-1.498-.732-.204-.075-.351-.112-.497.112-.148.224-.576.733-.706.885-.13.15-.26.168-.48.058-.22-.109-.929-.348-1.785-1.111-.667-.597-1.12-1.329-1.248-1.554-.129-.224-.015-.347.098-.458.102-.1.224-.261.336-.393.112-.132.148-.224.224-.374.075-.149.039-.28-.019-.393-.058-.112-.497-1.192-.681-1.631-.171-.41-.355-.363-.497-.363-.131 0-.28 0-.441 0-.16 0-.421.058-.642.302-.219.243-.842.831-.842 2.028 0 1.195.869 2.345.989 2.513.12.169 1.706 2.636 4.14 3.659 1.571.662 2.189.65 2.973.535.867-.129 1.296-.535 1.48-1.05.184-.515.184-.956.129-1.049-.054-.093-.201-.15-.42-.259z"/></svg>
                              </div>
                              <div className="flex-1 min-w-0">
                                 <h4 className="text-white font-bold text-sm mb-1">Unlock WhatsApp Detection</h4>
                                 <p className="text-gray-400 text-xs mb-3">Link your WhatsApp to see which contacts use it</p>
                                 <div className="inline-flex items-center gap-1.5 bg-brand/10 px-2 py-1 rounded text-[10px] font-medium text-brand">
                                    <svg className="w-2.5 h-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                                    Privacy First - No data stored
                                 </div>
                              </div>
                              <div className="text-gray-500 pt-1">
                                   <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true"><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
                              </div>
                           </div>
                        </div>
                     </div>
                     
                  </div>
                  
                  {/* Reflection & Sheen */}
                  <div className="absolute inset-0 bg-gradient-to-tr from-transparent via-white/5 to-transparent pointer-events-none z-30"></div>
                  
                  {/* Fade out bottom */}
                  <div className="absolute bottom-0 left-0 right-0 h-12 bg-gradient-to-t from-black to-transparent pointer-events-none z-20"></div>
                </div>
             </div>
             
             {/* Glow behind phone */}
             <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 w-full h-full bg-brand/20 blur-3xl -z-10 rounded-full opacity-40"></div>
          </div>

        </div>
      </div>
    </section>
  );
};
