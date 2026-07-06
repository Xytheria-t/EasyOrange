import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import Footer from './Footer';

describe('Footer', () => {
    it('renders brand name', () => {
        render(<Footer />);
        expect(screen.getByText('EasyOrange')).toBeInTheDocument();
    });

    it('renders copyright text', () => {
        render(<Footer />);
        expect(
            screen.getByText('© 2025-2026 EasyOrange — 让大模型在真实业务中稳定运行的全栈工程实践')
        ).toBeInTheDocument();
    });

    it('renders mini platform stats', () => {
        render(<Footer />);
        expect(screen.getByText('5,280+')).toBeInTheDocument();
        expect(screen.getByText('3,560+')).toBeInTheDocument();
        expect(screen.getByText('2,180+')).toBeInTheDocument();
    });
});
