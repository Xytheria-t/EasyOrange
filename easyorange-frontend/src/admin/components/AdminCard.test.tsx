import { describe, it, expect } from 'vitest';
import { screen } from '@testing-library/react';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { AdminCard, AdminCardStats } from './AdminCard';

describe('AdminCard', () => {
  it('renders children', () => {
    renderWithProviders(<AdminCard>卡片内容</AdminCard>);
    expect(screen.getByText('卡片内容')).toBeInTheDocument();
  });

  it('renders title when provided', () => {
    renderWithProviders(<AdminCard title="卡片标题">内容</AdminCard>);
    expect(screen.getByText('卡片标题')).toBeInTheDocument();
  });

  it('renders extra content when provided', () => {
    renderWithProviders(<AdminCard extra={<button>操作</button>}>内容</AdminCard>);
    expect(screen.getByRole('button', { name: '操作' })).toBeInTheDocument();
  });

  it('does not render header without title or extra', () => {
    const { container } = renderWithProviders(<AdminCard>内容</AdminCard>);
    expect(container.querySelector('[role="heading"]')).toBeNull();
  });
});

describe('AdminCardStats', () => {
  it('renders title and value', () => {
    renderWithProviders(<AdminCardStats title="总用户" value="100" />);
    expect(screen.getByText('总用户')).toBeInTheDocument();
    expect(screen.getByText('100')).toBeInTheDocument();
  });

  it('renders positive trend', () => {
    renderWithProviders(<AdminCardStats title="收入" value="5000" trend={{ value: 12.5, isPositive: true }} />);
    expect(screen.getByText('12.5%')).toBeInTheDocument();
    expect(screen.getByText('较昨日')).toBeInTheDocument();
  });

  it('renders negative trend', () => {
    renderWithProviders(<AdminCardStats title="收入" value="5000" trend={{ value: 5.3, isPositive: false }} />);
    expect(screen.getByText('5.3%')).toBeInTheDocument();
  });
});
