import { useCallback, useState } from 'react';
import { type AutoListingResult, aiApi } from '@/api/aiApi';
import { StreamError } from '@/api/core/stream';
import { useUIStore } from '@/store/uiStore';
import type { AgentStep } from '@/types/ai';

/**
 * 失败文案：后端失败（B8002 等）自带面向用户的说明，优先展示它；
 * 只有网络层/未知异常才回落到本地兜底话术 —— 用户至少知道「这次没成」。
 */
function failureMessage(error: unknown): string {
    if (error instanceof StreamError && error.message) {
        return error.message;
    }
    return 'AI 识别失败，请稍后重试';
}

/** done 事件载荷是 AutoListingResult 的 JSON 文本（对象序列化后 parseRaw 不剥壳，原样透传） */
function parseResult(raw: string): AutoListingResult | null {
    try {
        const parsed = JSON.parse(raw) as AutoListingResult;
        return parsed && typeof parsed === 'object' ? parsed : null;
    } catch {
        return null;
    }
}

export function useAutoListing() {
    const [result, setResult] = useState<AutoListingResult | null>(null);
    /** 识别链路的工具步骤 — 流式逐步流入，供思考过程面板可视化 */
    const [steps, setSteps] = useState<AgentStep[]>([]);
    const [isLoading, setIsLoading] = useState(false);
    /** 最近一次识别失败的文案 — 持久挂在识别按钮旁给重试入口，不随 toast 消失 */
    const [failure, setFailure] = useState<string | null>(null);
    const addToast = useUIStore(s => s.addToast);

    const analyzeImages = useCallback(
        async (imageUrls: string[]) => {
            setIsLoading(true);
            setFailure(null);
            // 重试清空上一轮步骤：面板原位重新流入，新旧步骤不接在一起
            setSteps([]);

            const fail = (message: string) => {
                setFailure(message);
                addToast({ type: 'error', message });
            };

            try {
                await aiApi.autoListingStream(imageUrls, event => {
                    switch (event.type) {
                        case 'step':
                            setSteps(prev => [...prev, event.data]);
                            break;
                        case 'done': {
                            const listing = parseResult(event.data);
                            if (listing) {
                                setResult(listing);
                                addToast({ type: 'success', message: 'AI 智能识别完成，已自动填充信息' });
                            } else {
                                fail('AI 识别失败，请稍后重试');
                            }
                            break;
                        }
                        case 'error':
                            fail(event.data);
                            break;
                        // 非发布链路的事件（后端空实现），到达也不渲染
                        case 'token':
                        case 'sources':
                            break;
                    }
                });
            } catch (error) {
                fail(failureMessage(error));
            } finally {
                setIsLoading(false);
            }
        },
        [addToast]
    );

    const clearResult = useCallback(() => {
        setResult(null);
        setFailure(null);
    }, []);

    return { result, steps, isLoading, failure, analyzeImages, clearResult };
}
