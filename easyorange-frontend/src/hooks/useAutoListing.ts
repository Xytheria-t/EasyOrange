import { useCallback, useState } from 'react';
import { type AutoListingResult, aiApi } from '@/api/aiApi';
import { ApiClientError } from '@/api/core/request';
import { useUIStore } from '@/store/uiStore';

/**
 * 失败文案：后端失败（B8002 等）自带面向用户的说明，优先展示它；
 * 只有网络层/未知异常才回落到本地兜底话术 —— 用户至少知道「这次没成」。
 */
function failureMessage(error: unknown): string {
    if (error instanceof ApiClientError && error.message) {
        return error.message;
    }
    return 'AI 识别失败，请稍后重试';
}

export function useAutoListing() {
    const [result, setResult] = useState<AutoListingResult | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const addToast = useUIStore(s => s.addToast);

    const analyzeImages = useCallback(
        async (imageUrls: string[]) => {
            setIsLoading(true);
            try {
                const result = await aiApi.autoListing(imageUrls);
                if (result.data) {
                    setResult(result.data);
                    addToast({ type: 'success', message: 'AI 智能识别完成，已自动填充信息' });
                } else {
                    addToast({ type: 'error', message: 'AI 识别失败，请稍后重试' });
                }
            } catch (error) {
                addToast({ type: 'error', message: failureMessage(error) });
            } finally {
                setIsLoading(false);
            }
        },
        [addToast]
    );

    const clearResult = useCallback(() => {
        setResult(null);
    }, []);

    return { result, isLoading, analyzeImages, clearResult };
}
