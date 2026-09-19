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

    it('renders both mainlines in column headers', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText('资产方侧 · 发布助手单入口')).toBeInTheDocument();
        expect(screen.getByText('认领方侧 · 对话式找货')).toBeInTheDocument();
    });

    it('renders five steps across both sides', () => {
        render(<AIFeaturesSection />);

        expect(screen.getAllByTestId(/^pipeline-step-/)).toHaveLength(5);
    });

    it('renders only citable stats', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText('2 条')).toBeInTheDocument();
        expect(screen.getByText('1 次')).toBeInTheDocument();
        expect(screen.getByText('4 路')).toBeInTheDocument();
        expect(screen.getByText('35 条')).toBeInTheDocument();
    });
});
