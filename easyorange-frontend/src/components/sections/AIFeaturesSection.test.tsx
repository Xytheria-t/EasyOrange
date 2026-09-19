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

    it('renders five steps across both sides', () => {
        render(<AIFeaturesSection />);

        expect(screen.getAllByTestId(/^pipeline-step-/)).toHaveLength(5);
    });

    it('renders only citable stats in the footnote', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText(/2 条主线链路/)).toBeInTheDocument();
        expect(screen.getByText(/发布路径 1 次模型调用/)).toBeInTheDocument();
        expect(screen.getByText(/4 路并行编排/)).toBeInTheDocument();
        expect(screen.getByText(/金标准 35 条进 CI/)).toBeInTheDocument();
    });
});
