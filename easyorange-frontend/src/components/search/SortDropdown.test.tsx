import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import SortDropdown from './SortDropdown';

describe('SortDropdown', () => {
  it('renders current value', () => {
    renderWithProviders(<SortDropdown value="newest" onChange={vi.fn()} />);
    expect(screen.getByText('最新发布')).toBeInTheDocument();
  });

  it('opens panel and calls onChange when selecting an option', async () => {
    const onChange = vi.fn();
    renderWithProviders(<SortDropdown value="newest" onChange={onChange} />);

    await userEvent.click(screen.getByRole('combobox'));
    await userEvent.click(screen.getByText('价格从高到低'));

    expect(onChange).toHaveBeenCalledWith('price_desc');
  });

  it('marks current option as selected', async () => {
    const onChange = vi.fn();
    renderWithProviders(<SortDropdown value="popular" onChange={onChange} />);

    await userEvent.click(screen.getByRole('combobox'));
    expect(screen.getByRole('option', { name: '最受欢迎' })).toHaveAttribute('data-state', 'checked');
  });
});
