import { describe, it, expect } from 'vitest';
import { render, screen } from '@testing-library/react';
import { MemoryRouter } from 'react-router-dom';
import HomePage from './HomePage';

// HeroSection renders without stores, just needs MemoryRouter
vi.mock('@/components/sections/HeroSection', () => ({
  default: () => <div data-testid="hero-section">HeroSection</div>,
}));

vi.mock('@/components/sections/AIFeaturesSection', () => ({
  default: () => <div data-testid="ai-features">AIFeatures</div>,
}));

vi.mock('@/components/sections/CategoriesSection', () => ({
  default: () => <div data-testid="categories">Categories</div>,
}));

vi.mock('@/components/sections/AIRecommendSection', () => ({
  default: () => <div data-testid="ai-recommend">AIRecommend</div>,
}));

vi.mock('@/components/sections/ProductsSection', () => ({
  default: () => <div data-testid="products-section">Products</div>,
}));

describe('HomePage', () => {
  it('renders HeroSection', () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );
    expect(screen.getByTestId('hero-section')).toBeInTheDocument();
  });

  it('renders lazy loaded sections', async () => {
    render(
      <MemoryRouter>
        <HomePage />
      </MemoryRouter>,
    );

    // Lazy loaded sections should appear after Suspense resolves
    expect(await screen.findByTestId('ai-features')).toBeInTheDocument();
    expect(await screen.findByTestId('categories')).toBeInTheDocument();
    expect(await screen.findByTestId('ai-recommend')).toBeInTheDocument();
    expect(await screen.findByTestId('products-section')).toBeInTheDocument();
  });
});
