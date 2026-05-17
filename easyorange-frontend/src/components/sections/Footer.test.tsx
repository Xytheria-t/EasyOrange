import { describe, it, expect, vi, beforeEach } from 'vitest';
import { render, screen } from '@testing-library/react';
import Footer from './Footer';

const mockGetPlatformStats = vi.fn();
vi.mock('@/api/statsApi', () => ({
  statsApi: {
    getPlatformStats: () => mockGetPlatformStats(),
  },
}));

describe('Footer', () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockGetPlatformStats.mockResolvedValue({
      code: 'A0000',
      data: { activeUsers: 1000, onlineProducts: 500, completedOrders: 300 },
    });
  });

  it('renders brand name', () => {
    render(<Footer />);
    expect(screen.getByText('EasyOrange')).toBeInTheDocument();
  });

  it('renders copyright text', () => {
    render(<Footer />);
    expect(screen.getByText('© 2026 EasyOrange · 易橙坊')).toBeInTheDocument();
  });

  it('renders stats section', () => {
    render(<Footer />);
    expect(screen.getByText('活跃用户')).toBeInTheDocument();
    expect(screen.getByText('成功交易')).toBeInTheDocument();
  });

  it('shows default stats when API fails', async () => {
    mockGetPlatformStats.mockRejectedValue(new Error('Network error'));
    render(<Footer />);
    // Two default stats show "0" (activeUsers=0, completedOrders=0)
    const zeros = screen.getAllByText('0');
    expect(zeros.length).toBe(2);
  });

  it('fetches and renders stats', async () => {
    render(<Footer />);
    // After the API call resolves, stats should show with + suffix
    expect(await screen.findByText('1,000+')).toBeInTheDocument();
    expect(await screen.findByText('300+')).toBeInTheDocument();
  });
});
