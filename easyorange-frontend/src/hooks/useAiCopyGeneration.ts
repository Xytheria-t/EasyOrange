import { useCallback, useState } from 'react';
import { aiApi, type CopyGenerationParams, type CopyGenerationResult } from '@/api/aiApi';
import { useUIStore } from '@/store/uiStore';

export function useAiCopyGeneration() {
    const [result, setResult] = useState<CopyGenerationResult | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const addToast = useUIStore(s => s.addToast);

    const generateCopy = useCallback(
        async (params: CopyGenerationParams) => {
            setIsLoading(true);
            try {
                const result = await aiApi.generateCopy(params);
                if (result.data) {
                    setResult(result.data);
                    addToast({ type: 'success', message: 'AI 文案生成完成' });
                }
            } catch {
                addToast({ type: 'error', message: 'AI 文案生成失败，请稍后重试' });
            } finally {
                setIsLoading(false);
            }
        },
        [addToast]
    );

    const clearResult = useCallback(() => {
        setResult(null);
    }, []);

    return { result, isLoading, generateCopy, clearResult };
}
