import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import TypingIndicator from './TypingIndicator';

describe('TypingIndicator', () => {
  it('renders nothing when visible is false', () => {
    const { container } = render(<TypingIndicator userName="Alice" visible={false} />);
    expect(container.innerHTML).toBe('');
  });

  it('shows userName text when visible is true', () => {
    render(<TypingIndicator userName="Alice" visible={true} />);
    expect(screen.getByText(/Alice/)).toBeInTheDocument();
    expect(screen.getByText(/正在输入/)).toBeInTheDocument();
  });

  it('has bouncing dot elements', () => {
    const { container } = render(<TypingIndicator userName="Bob" visible={true} />);
    const dots = container.querySelectorAll('.animate-bounce');
    expect(dots.length).toBe(3);
  });
});
