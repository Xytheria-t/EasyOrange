import { describe, it, expect, vi } from 'vitest';
import { render, screen, fireEvent } from '@testing-library/react';
import { AdminTable, type Column } from './AdminTable';

interface TestItem {
  id: number;
  name: string;
  age: number;
}

const columns: Column<TestItem>[] = [
  { key: 'id', title: 'ID', sortable: true },
  { key: 'name', title: 'Name', sortable: true },
  { key: 'age', title: 'Age', sortable: true },
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

  it('shows loading state', () => {
    render(<AdminTable columns={columns} data={[]} rowKey="id" loading={true} />);
    expect(screen.getByText('加载中...')).toBeInTheDocument();
  });

  it('calls onRowClick when row clicked', () => {
    const onRowClick = vi.fn();
    render(
      <AdminTable columns={columns} data={data} rowKey="id" onRowClick={onRowClick} />,
    );
    fireEvent.click(screen.getByText('Alice').closest('td') as HTMLTableCellElement);
    expect(onRowClick).toHaveBeenCalledWith(data[0]);
  });

  it('sorts ascending on first click', () => {
    render(<AdminTable columns={columns} data={data} rowKey="id" />);
    fireEvent.click(screen.getByText('Age'));
    const rows = screen.getAllByText(/Alice|Bob|Charlie/);
    // After ascending sort by age: Bob(25), Alice(30), Charlie(35)
    expect(rows[0]).toHaveTextContent('Bob');
  });

  it('sorts descending on second click', () => {
    render(<AdminTable columns={columns} data={data} rowKey="id" />);
    const ageHeader = screen.getByText('Age');
    fireEvent.click(ageHeader);
    fireEvent.click(ageHeader);
    const rows = screen.getAllByText(/Alice|Bob|Charlie/);
    // After descending sort by age: Charlie(35), Alice(30), Bob(25)
    expect(rows[0]).toHaveTextContent('Charlie');
  });

  it('removes sort on third click', () => {
    render(<AdminTable columns={columns} data={data} rowKey="id" />);
    const ageHeader = screen.getByText('Age');
    fireEvent.click(ageHeader);
    fireEvent.click(ageHeader);
    fireEvent.click(ageHeader);
    const rows = screen.getAllByText(/Alice|Bob|Charlie/);
    // Back to original order: Alice, Bob, Charlie
    expect(rows[0]).toHaveTextContent('Alice');
  });

  it('shows pagination when pagination prop is provided', () => {
    render(
      <AdminTable
        columns={columns}
        data={data}
        rowKey="id"
        pagination={{ current: 1, pageSize: 10, total: 100, onChange: vi.fn() }}
      />,
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
      />,
    );
    const page2 = screen.getAllByRole('button').find(b => b.textContent === '2');
    expect(page2).toBeDefined();
    fireEvent.click(page2 as HTMLElement);
    expect(onChange).toHaveBeenCalledWith(2);
  });

  it('uses custom empty text', () => {
    render(
      <AdminTable columns={columns} data={[]} rowKey="id" emptyText="什么都没有" />,
    );
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
});
