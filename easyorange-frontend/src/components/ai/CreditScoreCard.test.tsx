import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { CreditScoreCard } from './CreditScoreCard';
import type { CreditScoreResult } from '@/api/creditApi';

const mockExcellent: CreditScoreResult = {
  userId: 1,
  creditScore: 185,
  level: 'EXCELLENT',
  totalTrades: 50,
  completedTrades: 48,
  cancelledTrades: 2,
  totalReports: 0,
  confirmedReports: 0,
  reviewAvgRating: 4.8,
  tradeCompletionRate: 96.0,
  lastUpdated: '2026-05-22T10:00:00',
};

const mockLow: CreditScoreResult = {
  userId: 2,
  creditScore: 60,
  level: 'LOW',
  totalTrades: 10,
  completedTrades: 5,
  cancelledTrades: 5,
  totalReports: 3,
  confirmedReports: 2,
  reviewAvgRating: 2.5,
  tradeCompletionRate: 50.0,
  lastUpdated: '2026-05-22T10:00:00',
};

const mockBlacklist: CreditScoreResult = {
  userId: 3,
  creditScore: 20,
  level: 'BLACKLIST',
  totalTrades: 5,
  completedTrades: 0,
  cancelledTrades: 5,
  totalReports: 10,
  confirmedReports: 8,
  reviewAvgRating: 1.0,
  tradeCompletionRate: 0.0,
  lastUpdated: '2026-05-22T10:00:00',
};

describe('CreditScoreCard', () => {
  it('renders title', () => {
    renderWithProviders(<CreditScoreCard credit={mockExcellent} />);
    expect(screen.getByText('信用评分')).toBeInTheDocument();
  });

  it('renders score value and max', () => {
    renderWithProviders(<CreditScoreCard credit={mockExcellent} />);
    expect(screen.getByText('185')).toBeInTheDocument();
    expect(screen.getByText('/200')).toBeInTheDocument();
  });

  it('renders level label for EXCELLENT', () => {
    renderWithProviders(<CreditScoreCard credit={mockExcellent} />);
    expect(screen.getByText('优秀')).toBeInTheDocument();
  });

  it('renders level label for LOW', () => {
    renderWithProviders(<CreditScoreCard credit={mockLow} />);
    expect(screen.getByText('较低')).toBeInTheDocument();
  });

  it('renders level label for BLACKLIST', () => {
    renderWithProviders(<CreditScoreCard credit={mockBlacklist} />);
    expect(screen.getByText('黑名单')).toBeInTheDocument();
  });

  it('renders stats section', () => {
    renderWithProviders(<CreditScoreCard credit={mockExcellent} />);
    expect(screen.getByText('交易完成率')).toBeInTheDocument();
    expect(screen.getByText('96%')).toBeInTheDocument();
    expect(screen.getByText('成功交易')).toBeInTheDocument();
    expect(screen.getByText('48')).toBeInTheDocument();
    expect(screen.getByText('平均评分')).toBeInTheDocument();
    expect(screen.getByText('4.8')).toBeInTheDocument();
  });

  it('handles unknown level gracefully by defaulting to NORMAL', () => {
    const unknownLevel: CreditScoreResult = {
      ...mockExcellent,
      creditScore: 100,
      level: 'UNKNOWN',
    };
    renderWithProviders(<CreditScoreCard credit={unknownLevel} />);
    expect(screen.getByText('正常')).toBeInTheDocument();
  });

  it('formats average rating to one decimal place', () => {
    renderWithProviders(<CreditScoreCard credit={mockLow} />);
    expect(screen.getByText('2.5')).toBeInTheDocument();
  });

  it('renders zero values correctly', () => {
    const zeroCredit: CreditScoreResult = {
      userId: 4,
      creditScore: 0,
      level: 'BLACKLIST',
      totalTrades: 0,
      completedTrades: 0,
      cancelledTrades: 0,
      totalReports: 0,
      confirmedReports: 0,
      reviewAvgRating: 0,
      tradeCompletionRate: 0,
      lastUpdated: '2026-05-22T10:00:00',
    };
    renderWithProviders(<CreditScoreCard credit={zeroCredit} />);
    expect(screen.getByText('0%')).toBeInTheDocument();
    expect(screen.getByText('0.0')).toBeInTheDocument();
    // Multiple elements contain "0": credit score value, completed trades (0), cancelled trades (0)
    const zeroElements = screen.getAllByText('0');
    expect(zeroElements.length).toBeGreaterThanOrEqual(2);
  });
});
