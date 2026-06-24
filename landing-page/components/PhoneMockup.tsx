
import React from 'react';

export const PhoneMockup: React.FC = () => {
    return (
        <div className="flex-1 relative z-10 w-full max-w-sm lg:max-w-md mx-auto animate-float" aria-hidden="true" >
            {/* Abstract Phone Body */}
            <div className="relative rounded-[2.5rem] bg-gray-900 border-4 border-gray-800 shadow-2xl overflow-hidden aspect-[9/19]">
                {/* Screen Content */}
                <div className="absolute inset-0 bg-black flex flex-col font-sans">
                    {/* Status Bar */}
                    <div className="px-6 pt-4 pb-2 flex justify-between items-center text-xs font-medium text-white/60 z-20">
                        <span>00:04</span>
                        <div className="flex gap-1.5 items-center">
                            <svg className="w-4 h-4" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true" ><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 18c-4.41 0-8-3.59-8-8s3.59-8 8-8 8 3.59 8 8-3.59 8-8 8zm-1-13h2v6h-2zm0 8h2v2h-2z" /></svg>
                            <div className="w-5 h-2.5 rounded-[1px] border border-white/40 relative">
                                <div className="absolute inset-0.5 bg-white/40 w-[40%]"></div>
                            </div>
                        </div>
                    </div>

                    {/* App Header */}
                    <div className="px-5 py-3 flex items-center justify-between z-20">
                        <div className="flex items-center gap-4">
                            <svg className="w-6 h-6 text-white" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M10 19l-7-7m0 0l7-7m-7 7h18" /></svg>
                            <h1 className="text-xl font-bold text-white">Scan Results</h1>
                        </div>
                        <svg className="w-6 h-6 text-brand" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 8v4l3 3m6-3a9 9 0 11-18 0 9 9 0 0118 0z" /></svg>
                    </div>

                    {/* Content Scroll Area */}
                    <div className="flex-1 overflow-y-auto relative z-10 px-4 space-y-4 pt-2 no-scrollbar pb-6">
                        {/* Summary Card */}
                        <div className="bg-[#1c1c1e] rounded-2xl p-5 border border-white/5 shadow-lg">
                            <div className="flex items-start gap-4 mb-6">
                                <div className="w-12 h-12 rounded-full bg-blue-500/20 flex items-center justify-center shrink-0">
                                    <svg className="w-6 h-6 text-blue-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 11H5m14 0a2 2 0 012 2v6a2 2 0 01-2 2H5a2 2 0 01-2-2v-6a2 2 0 012-2m14 0V9a2 2 0 00-2-2M5 11V9a2 2 0 012-2m0 0V5a2 2 0 012-2h6a2 2 0 012 2v2M7 7h10" /></svg>
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
                                {[
                                    {
                                        id: 'format',
                                        icon: <svg className="w-5 h-5 text-cyan-400" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2V6zM14 6a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2V6zM4 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2H6a2 2 0 01-2-2v-2zM14 16a2 2 0 012-2h2a2 2 0 012 2v2a2 2 0 01-2 2h-2a2 2 0 01-2-2v-2z" /></svg>,
                                        title: "Format Issues",
                                        description: "Fixing Format issues helps identify...",
                                        count: "13,739",
                                        colorClass: "text-cyan-400",
                                        bgClass: "bg-[#111827]",
                                        iconBgClass: "bg-cyan-500/10",
                                        duration: "duration-500"
                                    },
                                    {
                                        id: 'sensitive',
                                        icon: <svg className="w-5 h-5 text-yellow-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>,
                                        title: "Sensitive Data",
                                        description: "Safety Check: Potential sensitive IDs...",
                                        count: "20",
                                        colorClass: "text-yellow-500",
                                        bgClass: "bg-[#1c1917]",
                                        iconBgClass: "bg-yellow-500/10",
                                        duration: "duration-700"
                                    }
                                ].map(issue => (
                                    <MockIssueCard key={issue.id} {...issue} />
                                ))}
                            </div>
                        </div>

                        {/* Issues Found Section */}
                        <div>
                            <h3 className="text-gray-500 text-xs font-bold uppercase tracking-widest mb-3 ml-1">Issues Found</h3>
                            <div className="space-y-3">
                                {[
                                    {
                                        id: 'junk',
                                        icon: <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 9v2m0 4h.01m-6.938 4h13.856c1.54 0 2.502-1.667 1.732-3L13.732 4c-.77-1.333-2.694-1.333-3.464 0L3.34 16c-.77 1.333.192 3 1.732 3z" /></svg>,
                                        title: "Junk Contacts",
                                        description: "Contacts with missing or invalid data",
                                        count: "2,501",
                                        colorClass: "text-red-500",
                                        bgClass: "bg-[#1f1212]",
                                        iconBgClass: "bg-red-500/20",
                                        duration: "duration-1000"
                                    },
                                    {
                                        id: 'missing',
                                        icon: <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M3 5a2 2 0 012-2h3.28a1 1 0 01.948.684l1.498 4.493a1 1 0 01-.502 1.21l-2.257 1.13a11.042 11.042 0 005.516 5.516l1.13-2.257a1 1 0 011.21-.502l4.493 1.498a1 1 0 01.684.949V19a2 2 0 01-2 2h-1C9.716 21 3 14.284 3 6V5z" /></svg>,
                                        title: "Missing Numbers",
                                        description: "Contacts without any phone numbers",
                                        count: "24",
                                        colorClass: "text-red-500",
                                        bgClass: "bg-[#1f1212]",
                                        iconBgClass: "bg-red-500/20",
                                        duration: "duration-1000",
                                        delay: "delay-100"
                                    },
                                    {
                                        id: 'duplicates',
                                        icon: <svg className="w-5 h-5 text-yellow-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z" /></svg>,
                                        title: "Duplicates",
                                        description: "Contacts that appear multiple times",
                                        count: "31,375",
                                        colorClass: "text-yellow-500",
                                        bgClass: "bg-[#1c1c1e]",
                                        iconBgClass: "bg-yellow-600/10",
                                        duration: "duration-1000",
                                        delay: "delay-200"
                                    },
                                    {
                                        id: 'numerical',
                                        icon: <svg className="w-5 h-5 text-red-500" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M4 6h16M4 12h16M4 18h7" /></svg>,
                                        title: "Numerical Names",
                                        description: "Names consisting only of digits",
                                        count: "991",
                                        colorClass: "text-red-500",
                                        bgClass: "bg-[#1f1212]",
                                        iconBgClass: "bg-red-500/20",
                                        duration: "duration-1000",
                                        delay: "delay-300"
                                    }
                                ].map(issue => (
                                    <MockIssueCard key={issue.id} {...issue} />
                                ))}
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
                                        <svg className="w-6 h-6 text-white" fill="currentColor" viewBox="0 0 24 24" aria-hidden="true" ><path d="M12.031 6.172c-3.181 0-5.767 2.586-5.768 5.766-.001 1.298.38 2.27 1.019 3.287l-.711 2.592 2.654-.696c1.001.572 2.073.811 3.195.811 3.18 0 5.767-2.587 5.767-5.766.001-3.185-2.58-5.794-6.156-5.794zm5.292 6.808c-.219-.109-1.296-.657-1.498-.732-.204-.075-.351-.112-.497.112-.148.224-.576.733-.706.885-.13.15-.26.168-.48.058-.22-.109-.929-.348-1.785-1.111-.667-.597-1.12-1.329-1.248-1.554-.129-.224-.015-.347.098-.458.102-.1.224-.261.336-.393.112-.132.148-.224.224-.374.075-.149.039-.28-.019-.393-.058-.112-.497-1.192-.681-1.631-.171-.41-.355-.363-.497-.363-.131 0-.28 0-.441 0-.16 0-.421.058-.642.302-.219.243-.842.831-.842 2.028 0 1.195.869 2.345.989 2.513.12.169 1.706 2.636 4.14 3.659 1.571.662 2.189.65 2.973.535.867-.129 1.296-.535 1.48-1.05.184-.515.184-.956.129-1.049-.054-.093-.201-.15-.42-.259z" /></svg>
                                    </div>
                                    <div className="flex-1 min-w-0">
                                        <h4 className="text-white font-bold text-sm mb-1">Unlock WhatsApp Detection</h4>
                                        <p className="text-gray-400 text-xs mb-3">Link your WhatsApp to see which contacts use it</p>
                                        <div className="inline-flex items-center gap-1.5 bg-brand/10 px-2 py-1 rounded text-[10px] font-medium text-brand">
                                            <svg className="w-2.5 h-2.5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M12 15v2m-6 4h12a2 2 0 002-2v-6a2 2 0 00-2-2H6a2 2 0 00-2 2v6a2 2 0 002 2zm10-10V7a4 4 0 00-8 0v4h8z" /></svg>
                                            Privacy First - No data stored
                                        </div>
                                    </div>
                                    <div className="text-gray-500 pt-1">
                                        <svg className="w-5 h-5" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
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
    );
};

interface MockIssueCardProps {
    icon: React.ReactNode;
    title: string;
    description: string;
    count: string;
    colorClass: string;
    bgClass: string;
    iconBgClass: string;
    duration: string;
    delay?: string;
}

const MockIssueCard: React.FC<MockIssueCardProps> = ({
    icon,
    title,
    description,
    count,
    colorClass,
    bgClass,
    iconBgClass,
    duration,
    delay = ""
}) => (
    <div className={`${bgClass} rounded-xl p-4 flex items-center gap-4 border border-white/5 animate-in slide-in-from-bottom-4 ${duration} ${delay}`}>
        <div className={`w-10 h-10 rounded-full ${iconBgClass} flex items-center justify-center shrink-0`}>
            {icon}
        </div>
        <div className="flex-1 min-w-0">
            <h4 className="text-white font-semibold text-sm">{title}</h4>
            <p className="text-gray-500 text-xs truncate">{description}</p>
        </div>
        <div className="text-right shrink-0">
            <span className={`${colorClass} font-bold text-lg`}>{count}</span>
        </div>
        <svg className="w-4 h-4 text-gray-600" fill="none" viewBox="0 0 24 24" stroke="currentColor" aria-hidden="true" ><path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" /></svg>
    </div>
);
