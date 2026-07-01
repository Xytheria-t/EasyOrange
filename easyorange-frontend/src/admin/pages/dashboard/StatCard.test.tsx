import { render, screen } from '@testing-library/react';
import { describe, expect, it } from 'vitest';
import { StatCard } from './StatCard';

describe('StatCard', () => {
    it('renders title and value', () => {
        render(<StatCard title="用户总数" value={1234} />);
        expect(screen.getByText('用户总数')).toBeInTheDocument();
        expect(screen.getByText('1,234')).toBeInTheDocument();
    });

    it('formats values over 10000', () => {
        render(<StatCard title="用户总数" value={15000} />);
        expect(screen.getByText('1.5万')).toBeInTheDocument();
    });

    it('renders string values directly', () => {
        render(<StatCard title="好评率" value="98.5%" />);
        expect(screen.getByText('98.5%')).toBeInTheDocument();
    });

    it('shows positive growth indicator', () => {
        render(<StatCard title="用户" value={100} growth={12.5} />);
        expect(screen.getByText('12.5%')).toBeInTheDocument();
        // Positive growth has up arrow SVG
        const container = screen.getByText('12.5%').closest('span');
        const upArrow = container?.querySelector('path[d="M18 15l-6-6-6 6"]');
        expect(upArrow).toBeInTheDocument();
    });

    it('shows negative growth indicator', () => {
        render(<StatCard title="用户" value={100} growth={-5} />);
        expect(screen.getByText('5%')).toBeInTheDocument();
        // Negative growth has down arrow SVG
        const container = screen.getByText('5%').closest('span');
        const downArrow = container?.querySelector('path[d="M6 9l6 6 6-6"]');
        expect(downArrow).toBeInTheDocument();
    });

    it('does not show growth when zero', () => {
        render(<StatCard title="用户" value={100} growth={0} />);
        expect(screen.queryByText('0%')).not.toBeInTheDocument();
    });

    it('shows sub text when no growth', () => {
        render(<StatCard title="用户" value={100} sub="昨日新增 10 人" />);
        expect(screen.getByText('昨日新增 10 人')).toBeInTheDocument();
    });

    it('renders icon when provided', () => {
        render(<StatCard title="用户" value={100} icon={<span data-testid="test-icon">🔔</span>} />);
        expect(screen.getByTestId('test-icon')).toBeInTheDocument();
    });
});
