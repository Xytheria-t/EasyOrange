import { useCallback, useState } from 'react';
import { aiApi, type QaRequest, type QaResponse } from '@/api/aiApi';

export interface QaHistoryItem {
    question: string;
    answer: QaResponse;
}

export function useAiQa() {
    const [qaHistory, setQaHistory] = useState<QaHistoryItem[]>([]);
    const [isLoading, setIsLoading] = useState(false);

    const ask = useCallback(async (request: QaRequest) => {
        setIsLoading(true);
        try {
            const response = await aiApi.answerQuestion(request);
            const item: QaHistoryItem = {
                question: request.question,
                answer: response.data
            };
            setQaHistory(prev => [...prev, item]);
        } catch {
            const errorItem: QaHistoryItem = {
                question: request.question,
                answer: { answer: '抱歉，AI 暂时无法回答，请稍后重试。', confidence: false }
            };
            setQaHistory(prev => [...prev, errorItem]);
        } finally {
            setIsLoading(false);
        }
    }, []);

    const clearHistory = useCallback(() => {
        setQaHistory([]);
    }, []);

    return { qaHistory, isLoading, ask, clearHistory };
}