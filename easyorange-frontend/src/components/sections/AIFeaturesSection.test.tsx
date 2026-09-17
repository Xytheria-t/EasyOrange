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
        expect(screen.getByText('六步闭环')).toBeInTheDocument();
        expect(screen.getByText('资产方省心')).toBeInTheDocument();
        expect(screen.getByText('认领方放心')).toBeInTheDocument();
    });

    it('renders both ends of the subtitle', () => {
        render(<AIFeaturesSection />);

        expect(screen.getByText('资产方侧')).toBeInTheDocument();
        expect(screen.getByText('认领方侧')).toBeInTheDocument();
    });

    it('renders three steps per side', () => {
        render(<AIFeaturesSection />);

        expect(screen.getAllByTestId(/^pipeline-step-/)).toHaveLength(6);
    });
});
