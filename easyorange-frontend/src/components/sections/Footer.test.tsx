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
        expect(screen.getByText('© 2025-2026 EasyOrange — 砍业务,撑架构')).toBeInTheDocument();
    });

    it('renders mini platform stats', () => {
        render(<Footer />);
        expect(screen.getByText('5,280+')).toBeInTheDocument();
        expect(screen.getByText('3,560+')).toBeInTheDocument();
        expect(screen.getByText('2,180+')).toBeInTheDocument();
    });
});
