import { useState, useMemo, useCallback } from 'react';

export interface Column<T> {
  key: keyof T | string;
  title: string;
  render?: (value: any, record: T) => React.ReactNode;
  sortable?: boolean;
}

export interface AdminTableProps<T> {
  columns: Column<T>[];
  data: T[];
  rowKey: keyof T;
  loading?: boolean;
  pagination?: {
    current: number;
    pageSize: number;
    total: number;
    onChange: (page: number) => void;
  };
  onRowClick?: (record: T) => void;
  emptyText?: string;
}

type SortDirection = 'asc' | 'desc' | null;

interface SortState {
  key: string | null;
  direction: SortDirection;
}

export function AdminTable<T extends Record<string, any>>({
  columns,
  data,
  rowKey,
  loading = false,
  pagination,
  onRowClick,
  emptyText = '暂无数据',
}: AdminTableProps<T>) {
  const [sortState, setSortState] = useState<SortState>({ key: null, direction: null });

  const handleSort = (columnKey: string) => {
    setSortState((prev) => {
      if (prev.key !== columnKey) return { key: columnKey, direction: 'asc' };
      if (prev.direction === 'asc') return { key: columnKey, direction: 'desc' };
      return { key: null, direction: null };
    });
  };

  const sortedData = useMemo(() => {
    if (!sortState.key || !sortState.direction) return data;
    return [...data].sort((a, b) => {
      const aValue = a[sortState.key as keyof T];
      const bValue = b[sortState.key as keyof T];
      if (aValue === bValue) return 0;
      if (aValue == null) return 1;
      if (bValue == null) return -1;
      const comparison = aValue < bValue ? -1 : 1;
      return sortState.direction === 'asc' ? comparison : -comparison;
    });
  }, [data, sortState]);

  const getValue = (record: T, key: keyof T | string): any => {
    if (typeof key === 'string' && key.includes('.')) {
      let value: any = record;
      for (const k of key.split('.')) value = value?.[k];
      return value;
    }
    return record[key as keyof T];
  };

  const handleRowMouseEnter = useCallback((e: React.MouseEvent<HTMLTableRowElement>, rIdx: number) => {
    const tr = e.currentTarget;
    Array.from(tr.children).forEach((cell) => {
      (cell as HTMLElement).style.background = 'rgba(249,115,22,0.025)';
      (cell as HTMLElement).style.borderBottomColor = rIdx < sortedData.length - 1 ? 'transparent' : '';
    });
  }, [sortedData.length]);

  const handleRowMouseLeave = useCallback((e: React.MouseEvent<HTMLTableRowElement>, rIdx: number) => {
    const tr = e.currentTarget;
    Array.from(tr.children).forEach((cell) => {
      (cell as HTMLElement).style.background = '';
      (cell as HTMLElement).style.borderBottomColor = rIdx < sortedData.length - 1 ? '#F5F2EE' : '';
    });
  }, [sortedData.length]);

  const totalPages = pagination ? Math.ceil(pagination.total / pagination.pageSize) : 0;

  const pageNumbers = useMemo(() => {
    if (!pagination) return [];
    const pages: (number | 'ellipsis')[] = [];
    const { current, pageSize, total } = pagination;
    const totalP = Math.ceil(total / pageSize);
    if (totalP <= 7) {
      for (let i = 1; i <= totalP; i++) pages.push(i);
    } else {
      pages.push(1);
      if (current > 3) pages.push('ellipsis');
      for (let i = Math.max(2, current - 1); i <= Math.min(totalP - 1, current + 1); i++) {
        pages.push(i);
      }
      if (current < totalP - 2) pages.push('ellipsis');
      pages.push(totalP);
    }
    return pages;
  }, [pagination, totalPages]);

  const renderSortIcon = (columnKey: string) => {
    const isActive = sortState.key === columnKey;
    return (
      <span style={{ marginLeft: 4, display: 'inline-flex', flexDirection: 'column', lineHeight: 1 }}>
        <svg
          style={{
            width: 10, height: 10, marginBottom: -2,
            color: isActive && sortState.direction === 'asc' ? '#F97316' : '#D6CEC5',
            transition: 'color 0.15s ease',
          }}
          fill="currentColor" viewBox="0 0 24 24"
        >
          <path d="M7 14l5-5 5 5z" />
        </svg>
        <svg
          style={{
            width: 10, height: 10,
            color: isActive && sortState.direction === 'desc' ? '#F97316' : '#D6CEC5',
            transition: 'color 0.15s ease',
          }}
          fill="currentColor" viewBox="0 0 24 24"
        >
          <path d="M7 10l5 5 5-5z" />
        </svg>
      </span>
    );
  };

  const thStyle: React.CSSProperties = {
    textAlign: 'left',
    padding: '0.85rem 1.25rem',
    fontSize: '0.68rem',
    fontWeight: 600,
    color: '#9B9590',
    textTransform: 'uppercase',
    letterSpacing: '0.06em',
    background: 'linear-gradient(180deg, rgba(249,115,22,0.04) 0%, transparent 100%)',
    borderBottom: '1px solid #EDE8E3',
    whiteSpace: 'nowrap',
  };

  const tdStyle: React.CSSProperties = {
    padding: '0.85rem 1.25rem',
    fontSize: '0.87rem',
    color: '#4A4540',
    borderBottom: '1px solid #F5F2EE',
    verticalAlign: 'middle',
    transition: 'background 0.15s ease',
  };

  return (
    <>
      {/* Table */}
      <div style={{ overflowX: 'auto' }}>
        <table style={{ width: '100%', borderCollapse: 'separate', borderSpacing: 0 }}>
          <thead>
            <tr>
              {columns.map((column, idx) => (
                <th
                  key={String(column.key)}
                  style={{
                    ...thStyle,
                    ...(idx === 0 ? { borderTopLeftRadius: 20 } : {}),
                    ...(idx === columns.length - 1 ? { borderTopRightRadius: 20 } : {}),
                    ...(column.sortable ? { cursor: 'pointer', userSelect: 'none' } : {}),
                  }}
                  onClick={() => column.sortable && handleSort(String(column.key))}
                >
                  <span style={{ display: 'inline-flex', alignItems: 'center' }}>
                    {column.title}
                    {column.sortable && renderSortIcon(String(column.key))}
                  </span>
                </th>
              ))}
            </tr>
          </thead>
          <tbody>
            {loading ? (
              <tr>
                <td colSpan={columns.length} style={{ ...tdStyle, textAlign: 'center' }}>
                  <div style={{ display: 'flex', flexDirection: 'column', alignItems: 'center', gap: '0.7rem', padding: '3rem 1rem' }}>
                    <div style={{
                      width: 28, height: 28,
                      border: '2.5px solid #E5E0DB',
                      borderTopColor: '#F97316',
                      borderRadius: '50%',
                      animation: 'spin 0.7s linear infinite',
                    }} />
                    <span style={{ fontSize: '0.87rem', color: '#9B9590' }}>加载中...</span>
                  </div>
                </td>
              </tr>
            ) : sortedData.length === 0 ? (
              <tr>
                <td colSpan={columns.length} style={{ ...tdStyle, textAlign: 'center' }}>
                  <div style={{ textAlign: 'center', padding: '3.5rem 1.5rem' }}>
                    <div style={{ fontSize: '2.2rem', marginBottom: '0.65rem', opacity: 0.45 }}>📭</div>
                    <div style={{ fontFamily: "'Playfair Display', serif", fontSize: '0.98rem', fontWeight: 600, color: '#8B857E', marginBottom: '0.25rem' }}>{emptyText}</div>
                    <div style={{ fontSize: '0.84rem', color: '#B5AEA8' }}>暂无相关数据</div>
                  </div>
                </td>
              </tr>
            ) : (
              sortedData.map((record, rIdx) => (
                <tr
                  key={String(record[rowKey])}
                  style={onRowClick ? { cursor: 'pointer', transition: 'background 0.15s ease' } : {}}
                  onClick={() => onRowClick?.(record)}
                  onMouseEnter={(e) => handleRowMouseEnter(e, rIdx)}
                  onMouseLeave={(e) => handleRowMouseLeave(e, rIdx)}
                >
                  {columns.map((column) => (
                    <td key={String(column.key)} style={tdStyle}>
                      {column.render
                        ? column.render(getValue(record, column.key), record)
                        : getValue(record, column.key)}
                    </td>
                  ))}
                </tr>
              ))
            )}
          </tbody>
        </table>
      </div>

      {/* Pagination */}
      {pagination && totalPages > 1 && (
        <div style={{
          display: 'flex', alignItems: 'center', justifyContent: 'space-between',
          padding: '0.9rem 1.25rem',
          borderTop: '1px solid #EDE8E3',
          position: 'relative',
        }}>
          {/* Decorative gradient line */}
          <div style={{
            position: 'absolute', top: 0, left: '1.25rem', right: '1.25rem', height: 1,
            background: 'linear-gradient(90deg, transparent, rgba(249,115,22,0.08), transparent)',
          }} />

          {/* Info */}
          <div style={{ fontSize: '0.81rem', color: '#B5AEA8' }}>
            共 <strong style={{ color: '#4A4540', fontWeight: 600 }}>{pagination.total.toLocaleString()}</strong> 条记录，
            第 <strong style={{ color: '#4A4540', fontWeight: 600 }}>{pagination.current}</strong> /{' '}
            <strong style={{ color: '#4A4540', fontWeight: 600 }}>{totalPages}</strong> 页
          </div>

          {/* Controls */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.35rem' }}>
            {/* Prev */}
            <button
              disabled={pagination.current <= 1}
              onClick={() => pagination.onChange(pagination.current - 1)}
              style={{
                minWidth: 34, height: 34,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                border: '1.5px solid transparent', borderRadius: 10,
                fontSize: '0.81rem', fontWeight: 600,
                color: '#8B857E', background: 'transparent',
                cursor: pagination.current <= 1 ? 'not-allowed' : 'pointer',
                transition: 'all 0.15s ease',
                opacity: pagination.current <= 1 ? 0.4 : 1,
                padding: '0 0.5rem',
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M15 18l-6-6 6-6" />
              </svg>
            </button>

            {/* Page numbers */}
            {pageNumbers.map((page, index) =>
              page === 'ellipsis' ? (
                <span key={`e-${index}`} style={{ display: 'inline-flex', alignItems: 'center', justifyContent: 'center', width: 34, height: 34, fontSize: '0.81rem', color: '#B5AEA8' }}>
                  ···
                </span>
              ) : (
                <button
                  key={page}
                  onClick={() => pagination.onChange(page)}
                  style={{
                    minWidth: 34, height: 34,
                    display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                    border: '1.5px solid transparent', borderRadius: 10,
                    fontSize: '0.81rem', fontWeight: 600,
                    color: page === pagination.current ? '#fff' : '#8B857E',
                    background: page === pagination.current ? 'linear-gradient(135deg, #F97316, #EA580C)' : 'transparent',
                    cursor: 'pointer', transition: 'all 0.15s ease',
                    boxShadow: page === pagination.current ? '0 2px 8px rgba(249,115,22,0.28)' : 'none',
                    padding: '0 0.5rem',
                  }}
                >
                  {page}
                </button>
              )
            )}

            {/* Next */}
            <button
              disabled={pagination.current >= totalPages}
              onClick={() => pagination.onChange(pagination.current + 1)}
              style={{
                minWidth: 34, height: 34,
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                border: '1.5px solid transparent', borderRadius: 10,
                fontSize: '0.81rem', fontWeight: 600,
                color: '#8B857E', background: 'transparent',
                cursor: pagination.current >= totalPages ? 'not-allowed' : 'pointer',
                transition: 'all 0.15s ease',
                opacity: pagination.current >= totalPages ? 0.4 : 1,
                padding: '0 0.5rem',
              }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M9 18l6-6-6-6" />
              </svg>
            </button>
          </div>
        </div>
      )}

      {/* Spinner animation */}
      <style>{`@keyframes spin { to { transform: rotate(360deg); } }`}</style>
    </>
  );
}
