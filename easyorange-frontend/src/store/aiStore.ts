import { create } from 'zustand';

interface AiState {
    isPricingLoading: boolean;
    isAutoListingLoading: boolean;
    isAiReviewLoading: boolean;
    isQaLoading: boolean;
    setPricingLoading: (loading: boolean) => void;
    setAutoListingLoading: (loading: boolean) => void;
    setAiReviewLoading: (loading: boolean) => void;
    setQaLoading: (loading: boolean) => void;
}

export const useAiStore = create<AiState>()((set) => ({
    isPricingLoading: false,
    isAutoListingLoading: false,
    isAiReviewLoading: false,
    isQaLoading: false,

    setPricingLoading: (loading) => set({ isPricingLoading: loading }),
    setAutoListingLoading: (loading) => set({ isAutoListingLoading: loading }),
    setAiReviewLoading: (loading) => set({ isAiReviewLoading: loading }),
    setQaLoading: (loading) => set({ isQaLoading: loading })
}));