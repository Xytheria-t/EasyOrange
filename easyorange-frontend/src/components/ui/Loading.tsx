import { Loader2 } from 'lucide-react';
import { useUIStore } from '@/store';

export function GlobalLoading() {
    const { isLoading, loadingMessage } = useUIStore();

    if (!isLoading) {
        return null;
    }

    return (
        <div className="fixed inset-0 z-[9999] flex items-center justify-center p-8">
            <div className="absolute inset-0 bg-white/85 backdrop-blur-xl" />
            <div className="relative z-10 flex flex-col items-center rounded-3xl border border-white/60 bg-white/80 px-12 py-10 shadow-xl backdrop-blur-2xl animate-in zoom-in-95 fade-in duration-500">
                <div className="relative mb-6 flex h-20 w-20 items-center justify-center">
                    <Loader2 className="absolute h-full w-full animate-spin text-primary" strokeWidth={1.5} />
                    <svg viewBox="0 0 48 48" fill="none" className="relative h-10 w-10">
                        <defs>
                            <linearGradient id="loadingLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                <stop offset="0%" stopColor="#F97316" />
                                <stop offset="40%" stopColor="#FB7185" />
                                <stop offset="100%" stopColor="#C39BD3" />
                            </linearGradient>
                        </defs>
                        <path
                            d="M8 12h13M8 12v24M8 36h13M8 12h8M8 24h10"
                            stroke="url(#loadingLogoGradient)"
                            strokeWidth="3"
                            strokeLinecap="round"
                            strokeLinejoin="round"
                        />
                        <circle cx="36" cy="24" r="10" stroke="url(#loadingLogoGradient)" strokeWidth="3" />
                    </svg>
                </div>
                <p className="text-base font-medium text-foreground">{loadingMessage || '加载中...'}</p>
            </div>
        </div>
    );
}
