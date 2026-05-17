import { describe, it, expect, beforeEach } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { useUIStore } from '@/store';
import { GlobalLoading } from './Loading';

beforeEach(() => {
  useUIStore.setState({ isLoading: false, loadingMessage: '', toasts: [] });
});

describe('GlobalLoading', () => {
  it('renders nothing when not loading', () => {
    const { container } = renderWithProviders(<GlobalLoading />);
    expect(container.firstChild).toBeNull();
  });

  it('renders loading overlay when isLoading is true', () => {
    useUIStore.setState({ isLoading: true });
    renderWithProviders(<GlobalLoading />);
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  it('renders custom loading message', () => {
    useUIStore.setState({ isLoading: true, loadingMessage: '正在上传...' });
    renderWithProviders(<GlobalLoading />);
    expect(screen.getByText('正在上传...')).toBeInTheDocument();
  });
});
