import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import ScrollProgressBar from './ScrollProgressBar';

describe('ScrollProgressBar', () => {
  beforeEach(() => {
    // Reset scroll and window dimensions
    window.scrollY = 0;
    Object.defineProperty(document.documentElement, 'scrollHeight', {
      value: 2000,
      writable: true,
    });
    Object.defineProperty(window, 'innerHeight', {
      value: 800,
      writable: true,
    });

    // Mock requestAnimationFrame to execute immediately
    vi.spyOn(window, 'requestAnimationFrame').mockImplementation((cb) => {
      cb(0);
      return 0;
    });
  });

  it('renders with progressbar role', () => {
    render(<ScrollProgressBar />);
    expect(screen.getByRole('progressbar')).toBeInTheDocument();
  });

  it('has aria-label for accessibility', () => {
    render(<ScrollProgressBar />);
    expect(screen.getByLabelText('页面滚动进度')).toBeInTheDocument();
  });

  it('has data-visible attribute', () => {
    const { container } = render(<ScrollProgressBar />);
    const element = container.querySelector('.scroll-progress-container');
    expect(element).toHaveAttribute('data-visible');
  });

  it('has progress bar fill element', () => {
    const { container } = render(<ScrollProgressBar />);
    expect(container.querySelector('.scroll-progress-fill')).toBeInTheDocument();
  });
});
