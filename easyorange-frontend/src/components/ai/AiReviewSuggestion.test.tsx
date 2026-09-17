import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { describe, expect, it, vi } from 'vitest';
import type { AiReviewResult } from '@/api/aiApi';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { AiReviewSuggestion } from './AiReviewSuggestion';

function result(overrides: Partial<AiReviewResult> = {}): AiReviewResult {
    return {
        isApproved: true,
        suggestedActionDesc: '通过',
        confidenceScore: 88,
        riskFlags: [],
        reasoning: '信息完整',
        ...overrides,
    };
}

describe('AiReviewSuggestion', () => {
    it('建议通过时可一键采纳（approve）', async () => {
        const onApply = vi.fn();
        renderWithProviders(
            <AiReviewSuggestion result={result()} isLoading={false} onGetSuggestion={vi.fn()} onApply={onApply} />
        );

        expect(screen.getByText('建议通过')).toBeInTheDocument();
        await userEvent.click(screen.getByRole('button', { name: /采纳 AI 建议/ }));

        expect(onApply).toHaveBeenCalledWith('approve');
    });

    it('建议拒绝时可一键采纳（reject）', async () => {
        const onApply = vi.fn();
        renderWithProviders(
            <AiReviewSuggestion
                result={result({ isApproved: false, suggestedActionDesc: '拒绝' })}
                isLoading={false}
                onGetSuggestion={vi.fn()}
                onApply={onApply}
            />
        );

        await userEvent.click(screen.getByRole('button', { name: /采纳 AI 建议/ }));

        expect(onApply).toHaveBeenCalledWith('reject');
    });

    it('AI 不可用时不给「采纳」按钮，也不渲染成拒绝态', () => {
        const onApply = vi.fn();
        renderWithProviders(
            <AiReviewSuggestion
                result={result({
                    isApproved: false,
                    suggestedActionDesc: '无法判定',
                    confidenceScore: 0,
                    riskFlags: ['AI_UNAVAILABLE'],
                    reasoning: 'AI 分析异常，请人工审核',
                })}
                isLoading={false}
                onGetSuggestion={vi.fn()}
                onApply={onApply}
            />
        );

        expect(screen.getByText('无法判定，请人工审核')).toBeInTheDocument();
        expect(screen.queryByText('建议拒绝')).not.toBeInTheDocument();
        expect(screen.queryByRole('button', { name: /采纳 AI 建议/ })).not.toBeInTheDocument();
        expect(onApply).not.toHaveBeenCalled();
    });
});
