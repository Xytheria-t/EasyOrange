import { describe, it, expect, vi } from 'vitest';
import { screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { AiPricingBadge } from './AiPricingBadge';
import type { PricingSuggestion } from '@/api/aiApi';

const mockSuggestion: PricingSuggestion = {
  suggestedPrice: 128.5,
  minPrice: 100,
  maxPrice: 150,
  reasoning: '同类商品均价120元，根据成色和品牌溢价建议定价128.5元',
  marketContext: '近7天有15件同类商品成交',
};

describe('AiPricingBadge', () => {
  it('renders loading state', () => {
    renderWithProviders(
      <AiPricingBadge suggestion={mockSuggestion} onApply={vi.fn()} isLoading />,
    );
    expect(screen.getByText('AI 正在分析市场行情...')).toBeInTheDocument();
  });

  it('renders normal state with pricing info', () => {
    renderWithProviders(
      <AiPricingBadge suggestion={mockSuggestion} onApply={vi.fn()} />,
    );
    expect(screen.getByText('AI 智能定价建议')).toBeInTheDocument();
    expect(screen.getByText('¥128.50')).toBeInTheDocument();
    expect(screen.getByText('¥100.00 - ¥150.00')).toBeInTheDocument();
    expect(screen.getByText(mockSuggestion.reasoning)).toBeInTheDocument();
    expect(screen.getByText(mockSuggestion.marketContext)).toBeInTheDocument();
  });

  it('fires onApply with suggested price on button click', async () => {
    const handleApply = vi.fn();
    renderWithProviders(
      <AiPricingBadge suggestion={mockSuggestion} onApply={handleApply} />,
    );
    await userEvent.click(screen.getByText('采纳此定价'));
    expect(handleApply).toHaveBeenCalledTimes(1);
    expect(handleApply).toHaveBeenCalledWith(128.5);
  });

  it('does not show pricing when loading', () => {
    renderWithProviders(
      <AiPricingBadge suggestion={mockSuggestion} onApply={vi.fn()} isLoading />,
    );
    expect(screen.queryByText('AI 智能定价建议')).not.toBeInTheDocument();
    expect(screen.queryByText('采纳此定价')).not.toBeInTheDocument();
  });

  it('renders with integer price values', () => {
    const intSuggestion: PricingSuggestion = {
      suggestedPrice: 100,
      minPrice: 80,
      maxPrice: 120,
      reasoning: 'reasonable',
      marketContext: 'market data',
    };
    renderWithProviders(
      <AiPricingBadge suggestion={intSuggestion} onApply={vi.fn()} />,
    );
    expect(screen.getByText('¥100.00')).toBeInTheDocument();
    expect(screen.getByText('¥80.00 - ¥120.00')).toBeInTheDocument();
  });
});
