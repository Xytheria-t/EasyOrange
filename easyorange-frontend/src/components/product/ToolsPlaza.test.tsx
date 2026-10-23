import { fireEvent, render, screen } from '@testing-library/react';
import { MemoryRouter, useLocation } from 'react-router-dom';
import { describe, expect, it, vi } from 'vitest';
import { ToolsPlaza } from './ToolsPlaza';

function LocationProbe() {
    const location = useLocation();
    return <div data-testid="location">{location.pathname}</div>;
}

function renderPlaza(ui: React.ReactElement) {
    return render(
        <MemoryRouter>
            {ui}
            <LocationProbe />
        </MemoryRouter>
    );
}

describe('ToolsPlaza', () => {
    it('renders brand title', () => {
        renderPlaza(<ToolsPlaza />);
        expect(screen.getByText('筛选工具')).toBeInTheDocument();
    });

    it('shows total count', () => {
        renderPlaza(<ToolsPlaza total={42} />);
        expect(screen.getByText('42 件商品')).toBeInTheDocument();
    });

    it('shows zero total by default', () => {
        renderPlaza(<ToolsPlaza />);
        expect(screen.getByText('0 件商品')).toBeInTheDocument();
    });

    it('renders filter buttons without sort options', () => {
        renderPlaza(<ToolsPlaza />);
        expect(screen.getByText('全部')).toBeInTheDocument();
        expect(screen.getByText('特价优惠')).toBeInTheDocument();
        expect(screen.queryByText('最新发布')).not.toBeInTheDocument();
        expect(screen.queryByText('热门商品')).not.toBeInTheDocument();
    });

    it('navigates to the AI playground instead of faking a filter', () => {
        renderPlaza(<ToolsPlaza />);
        fireEvent.click(screen.getByText('AI 找货'));
        expect(screen.getByTestId('location')).toHaveTextContent('/playground');
    });

    it('calls onFilterChange with "all" when clicking 全部', () => {
        const onFilterChange = vi.fn();
        renderPlaza(<ToolsPlaza onFilterChange={onFilterChange} />);
        fireEvent.click(screen.getByText('全部'));
        expect(onFilterChange).toHaveBeenCalledWith('all');
    });

    it('calls onFilterChange with "discount" when clicking 特价优惠', () => {
        const onFilterChange = vi.fn();
        renderPlaza(<ToolsPlaza onFilterChange={onFilterChange} />);
        fireEvent.click(screen.getByText('特价优惠'));
        expect(onFilterChange).toHaveBeenCalledWith('discount');
    });

    it('sets 全部 active by default', () => {
        renderPlaza(<ToolsPlaza />);
        const allBtn = screen.getByText('全部').closest('button');
        expect(allBtn?.className).toContain('active');
    });
});
