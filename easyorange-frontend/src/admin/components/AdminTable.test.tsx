import { fireEvent, render, screen } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AdminTable, type Column } from './AdminTable';

interface TestItem {
    id: number;
    name: string;
    age: number;
}

const columns: Column<TestItem>[] = [
    { key: 'id', title: 'ID' },
    { key: 'name', title: 'Name' },
    { key: 'age', title: 'Age' },
];

const data: TestItem[] = [
    { id: 1, name: 'Alice', age: 30 },
    { id: 2, name: 'Bob', age: 25 },
    { id: 3, name: 'Charlie', age: 35 },
];

describe('AdminTable', () => {
    it('renders column headers', () => {
        render(<AdminTable columns={columns} data={data} rowKey="id" />);
        expect(screen.getByText('ID')).toBeInTheDocument();
        expect(screen.getByText('Name')).toBeInTheDocument();
        expect(screen.getByText('Age')).toBeInTheDocument();
    });

    it('renders data rows', () => {
        render(<AdminTable columns={columns} data={data} rowKey="id" />);
        expect(screen.getByText('Alice')).toBeInTheDocument();
        expect(screen.getByText('Bob')).toBeInTheDocument();
        expect(screen.getByText('Charlie')).toBeInTheDocument();
    });

    it('shows empty state when no data', () => {
        render(<AdminTable columns={columns} data={[]} rowKey="id" />);
        expect(screen.getByText('暂无数据')).toBeInTheDocument();
    });

    it('shows a skeleton with an accessible busy status while loading', () => {
        render(<AdminTable columns={columns} data={[]} rowKey="id" loading={true} />);
        expect(screen.getByRole('status')).toHaveAttribute('aria-busy', 'true');
        expect(screen.getByText('加载中')).toBeInTheDocument();
    });

    it('calls onRowClick when row clicked', () => {
        const onRowClick = vi.fn();
        render(<AdminTable columns={columns} data={data} rowKey="id" onRowClick={onRowClick} />);
        fireEvent.click(screen.getByText('Alice').closest('td') as HTMLTableCellElement);
        expect(onRowClick).toHaveBeenCalledWith(data[0]);
    });

    it('shows pagination when pagination prop is provided', () => {
        render(
            <AdminTable
                columns={columns}
                data={data}
                rowKey="id"
                pagination={{ current: 1, pageSize: 10, total: 100, onChange: vi.fn() }}
            />
        );
        expect(screen.getByText(/共/)).toBeInTheDocument();
        expect(screen.getByText(/100/)).toBeInTheDocument();
        // Should show page number buttons (1, 2, 3, ..., 10)
        const page1 = screen.getAllByRole('button').find(b => b.textContent === '1');
        expect(page1).toBeDefined();
    });

    it('calls pagination onChange when page button clicked', () => {
        const onChange = vi.fn();
        render(
            <AdminTable
                columns={columns}
                data={data}
                rowKey="id"
                pagination={{ current: 1, pageSize: 10, total: 50, onChange }}
            />
        );
        const page2 = screen.getAllByRole('button').find(b => b.textContent === '2');
        expect(page2).toBeDefined();
        fireEvent.click(page2 as HTMLElement);
        expect(onChange).toHaveBeenCalledWith(2);
    });

    it('uses custom empty text', () => {
        render(<AdminTable columns={columns} data={[]} rowKey="id" emptyText="什么都没有" />);
        expect(screen.getByText('什么都没有')).toBeInTheDocument();
    });

    it('renders with custom render function', () => {
        const customColumns: Column<TestItem>[] = [
            {
                key: 'name',
                title: 'Name',
                render: (value: unknown) => `Mr. ${value}`,
            },
        ];
        render(<AdminTable columns={customColumns} data={data} rowKey="id" />);
        expect(screen.getByText('Mr. Alice')).toBeInTheDocument();
    });

    // ── 表头 ──
    // 客户端排序只作用于当前页，而分页是服务端的——排序结果会被误读成全量有序，砍掉。
    it('renders plain headers without sort affordances', () => {
        render(<AdminTable columns={columns} data={data} rowKey="id" />);
        expect(screen.getByRole('columnheader', { name: 'Age' })).not.toHaveAttribute('aria-sort');
        expect(screen.queryByRole('button', { name: /Age/ })).not.toBeInTheDocument();
    });

    it('activates clickable rows with Enter and Space', () => {
        const onRowClick = vi.fn();
        render(<AdminTable columns={columns} data={data} rowKey="id" onRowClick={onRowClick} />);
        const row = screen.getByText('Alice').closest('tr') as HTMLTableRowElement;

        expect(row).toHaveAttribute('tabindex', '0');
        fireEvent.keyDown(row, { key: 'Enter' });
        expect(onRowClick).toHaveBeenCalledWith(data[0]);

        fireEvent.keyDown(row, { key: ' ' });
        expect(onRowClick).toHaveBeenCalledTimes(2);
    });

    it('does not make rows focusable when onRowClick is absent', () => {
        render(<AdminTable columns={columns} data={data} rowKey="id" />);
        const row = screen.getByText('Alice').closest('tr') as HTMLTableRowElement;
        expect(row).not.toHaveAttribute('tabindex');
    });

    // ── 错误态 ──
    it('shows an error alert instead of empty state when error is set', () => {
        render(<AdminTable columns={columns} data={[]} rowKey="id" error={new Error('服务不可用')} />);
        expect(screen.getByRole('alert')).toBeInTheDocument();
        expect(screen.getByText('服务不可用')).toBeInTheDocument();
        expect(screen.queryByText('暂无数据')).not.toBeInTheDocument();
    });

    it('calls onRetry from the error state', () => {
        const onRetry = vi.fn();
        render(<AdminTable columns={columns} data={[]} rowKey="id" error={new Error('炸了')} onRetry={onRetry} />);
        fireEvent.click(screen.getByRole('button', { name: '重新加载' }));
        expect(onRetry).toHaveBeenCalledTimes(1);
    });

    it('prefers loading over error while a request is in flight', () => {
        render(<AdminTable columns={columns} data={[]} rowKey="id" loading error={new Error('炸了')} />);
        expect(screen.getByText('加载中')).toBeInTheDocument();
        expect(screen.queryByRole('alert')).not.toBeInTheDocument();
    });
});
