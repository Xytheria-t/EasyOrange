import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import TypingIndicator from './TypingIndicator';

describe('TypingIndicator', () => {
    it('renders nothing when visible is false', () => {
        const { container } = render(<TypingIndicator userName="Alice" isVisible={false} />);
        expect(container.innerHTML).toBe('');
    });

    it('shows userName text when visible is true', () => {
        render(<TypingIndicator userName="Alice" isVisible={true} />);
        expect(screen.getByText(/Alice/)).toBeInTheDocument();
        expect(screen.getByText(/正在输入/)).toBeInTheDocument();
    });

    it('has bouncing dot elements', () => {
        const { container } = render(<TypingIndicator userName="Bob" isVisible={true} />);
        const dots = container.querySelectorAll('.typing-dot');
        expect(dots.length).toBe(3);
    });
});
