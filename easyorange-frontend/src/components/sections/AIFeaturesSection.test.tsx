import { render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import AIFeaturesSection from './AIFeaturesSection';

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

describe('AIFeaturesSection', () => {
    it('renders header badge, label and two-tone title', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText('AI 工程化')).toBeInTheDocument();
        expect(screen.getByText('两条主线')).toBeInTheDocument();
        expect(screen.getByText('资产方省心')).toBeInTheDocument();
        expect(screen.getByText('认领方放心')).toBeInTheDocument();
    });

    it('renders two swimlanes grouped by mainline', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText('资产方')).toBeInTheDocument();
        expect(screen.getByText('认领方')).toBeInTheDocument();
        expect(screen.getByText('发布助手单入口')).toBeInTheDocument();
        expect(screen.getByText('对话式找货')).toBeInTheDocument();
    });

    it('renders four steps across both sides', () => {
        render(<AIFeaturesSection />);

        expect(screen.getAllByTestId(/^pipeline-step-/)).toHaveLength(4);
    });

    it('renders only citable stats in the footnote', () => {
        const { container } = render(<AIFeaturesSection />);

        const metrics = container.querySelector('.pipeline-metrics');
        expect(metrics?.textContent).toContain('2 条主线链路');
        expect(metrics?.textContent).toContain('发布路径 1 次模型调用');
        expect(metrics?.textContent).toContain('Agent 工具循环 7 步上限');
        expect(metrics?.textContent).toContain('金标准 43 条进 CI');
        expect(container.querySelectorAll('.metric-chip')).toHaveLength(4);
    });
});
