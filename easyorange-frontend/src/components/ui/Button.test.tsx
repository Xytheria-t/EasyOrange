import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { Button } from './Button';

describe('Button', () => {
  it('renders children text', () => {
    renderWithProviders(<Button>点击我</Button>);
    expect(screen.getByRole('button', { name: /点击我/i })).toBeInTheDocument();
  });

  it('applies variant class', () => {
    const { container } = renderWithProviders(<Button variant="primary">Primary</Button>);
    expect(container.querySelector('.btn-primary')).toBeInTheDocument();
  });

  it('applies size class', () => {
    const { container } = renderWithProviders(<Button size="lg">Large</Button>);
    expect(container.querySelector('.btn-lg')).toBeInTheDocument();
  });

  it('shows loading state and disables button', () => {
    const { container } = renderWithProviders(<Button isLoading>Loading</Button>);
    const button = screen.getByRole('button');
    expect(button).toBeDisabled();
    expect(container.querySelector('.btn-spinner')).toBeInTheDocument();
  });

  it('disables button when disabled prop is true', () => {
    renderWithProviders(<Button disabled>Disabled</Button>);
    expect(screen.getByRole('button')).toBeDisabled();
  });

  it('fires onClick handler', async () => {
    const handleClick = vi.fn();
    renderWithProviders(<Button onClick={handleClick}>Click</Button>);
    await userEvent.click(screen.getByRole('button'));
    expect(handleClick).toHaveBeenCalledTimes(1);
  });

  it('does not fire onClick when disabled', async () => {
    const handleClick = vi.fn();
    renderWithProviders(<Button onClick={handleClick} disabled>Click</Button>);
    await userEvent.click(screen.getByRole('button'));
    expect(handleClick).not.toHaveBeenCalled();
  });

  it('defaults type to button', () => {
    renderWithProviders(<Button>Default</Button>);
    expect(screen.getByRole('button')).toHaveAttribute('type', 'button');
  });
});
