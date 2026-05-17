import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { ToolsPlaza } from './ToolsPlaza';

describe('ToolsPlaza', () => {
  it('renders brand title', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('筛选工具')).toBeInTheDocument();
  });

  it('shows total count', () => {
    render(<ToolsPlaza total={42} />);
    expect(screen.getByText('42 件商品')).toBeInTheDocument();
  });

  it('shows zero total by default', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('0 件商品')).toBeInTheDocument();
  });

  it('renders all filter buttons', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('全部')).toBeInTheDocument();
    expect(screen.getByText('最新发布')).toBeInTheDocument();
    expect(screen.getByText('热门商品')).toBeInTheDocument();
    expect(screen.getByText('特价优惠')).toBeInTheDocument();
  });

  it('renders AI推荐 button', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('AI推荐')).toBeInTheDocument();
  });

  it('calls onFilterChange when clicking filter', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('最新发布'));
    expect(onFilterChange).toHaveBeenCalledWith('new');
  });

  it('calls onFilterChange with "ai" when clicking AI button', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledWith('ai');
  });

  it('shows AI hint when AI mode is active', () => {
    render(<ToolsPlaza />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(screen.getByText(/AI正在根据您的浏览习惯/)).toBeInTheDocument();
  });

  it('hides AI hint when clicking regular filter after AI mode', () => {
    render(<ToolsPlaza />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(screen.getByText(/AI正在根据您的浏览习惯/)).toBeInTheDocument();
    fireEvent.click(screen.getByText('全部'));
    expect(screen.queryByText(/AI正在根据您的浏览习惯/)).not.toBeInTheDocument();
  });

  it('sets all active by default', () => {
    render(<ToolsPlaza />);
    // "全部" should have active class (aiMode=false and activeFilter='all')
    const allBtn = screen.getByText('全部').closest('button');
    expect(allBtn?.className).toContain('active');
  });

  it('toggles AI mode on second click', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledWith('ai');
    fireEvent.click(screen.getByText('AI推荐'));
    // On second click, aiMode toggles to false and calls onFilterChange with 'ai' again
    expect(onFilterChange).toHaveBeenCalledTimes(2);
  });
});
