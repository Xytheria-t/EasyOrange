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

  it('renders filter buttons without sort options', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('全部')).toBeInTheDocument();
    expect(screen.getByText('特价优惠')).toBeInTheDocument();
    expect(screen.queryByText('最新发布')).not.toBeInTheDocument();
    expect(screen.queryByText('热门商品')).not.toBeInTheDocument();
  });

  it('renders AI推荐 button', () => {
    render(<ToolsPlaza />);
    expect(screen.getByText('AI推荐')).toBeInTheDocument();
  });

  it('calls onFilterChange with "all" when clicking 全部', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('全部'));
    expect(onFilterChange).toHaveBeenCalledWith('all');
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

  it('sets 全部 active by default', () => {
    render(<ToolsPlaza />);
    const allBtn = screen.getByText('全部').closest('button');
    expect(allBtn?.className).toContain('active');
  });

  it('toggles AI mode on second click', () => {
    const onFilterChange = vi.fn();
    render(<ToolsPlaza onFilterChange={onFilterChange} />);
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledWith('ai');
    fireEvent.click(screen.getByText('AI推荐'));
    expect(onFilterChange).toHaveBeenCalledTimes(2);
  });
});
