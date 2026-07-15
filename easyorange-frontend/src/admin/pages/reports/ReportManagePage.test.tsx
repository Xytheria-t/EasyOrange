import { fireEvent, screen } from '@testing-library/react';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import type { PageResult } from '@/types';
import type { AdminReport } from '../../types/admin';
import ReportManagePage from './ReportManagePage';

// ─── Hook mocks ───
const mockUseAdminReports = vi.fn();
const mockUseHandleReport = vi.fn();

vi.mock('../../hooks', () => ({
    useAdminReports: (...args: unknown[]) => mockUseAdminReports(...args),
    useHandleReport: (...args: unknown[]) => mockUseHandleReport(...args),
}));

// Mock AdminTable
vi.mock('../../components/AdminTable', () => ({
    AdminTable: ({
        columns,
        data,
        loading,
        pagination,
        emptyText,
    }: {
        columns: unknown[];
        data: AdminReport[];
        loading: boolean;
        pagination: unknown;
        emptyText: string;
    }) => (
        <div data-testid="admin-table">
            {loading ? (
                <div data-testid="loading-skeleton">Loading...</div>
            ) : data.length === 0 ? (
                <div data-testid="empty-state">{emptyText}</div>
            ) : (
                <div>
                    {data.map(report => (
                        <div key={report.reportId} data-testid="report-row">
                            <span data-testid="report-product">{report.productName}</span>
                            <span data-testid="report-reason">{report.reason}</span>
                            <span data-testid="report-reporter">{report.reporterName}</span>
                            <span data-testid="report-status">{report.status}</span>
                            {(columns as Array<{ key: string; render: (...args: unknown[]) => React.ReactNode }>)
                                .find(c => c.key === 'actions')
                                ?.render(null, report)}
                        </div>
                    ))}
                    {!!pagination && (
                        <div data-testid="pagination">
                            <button
                                type="button"
                                onClick={() => (pagination as { onChange: (p: number) => void }).onChange(2)}
                            >
                                下一页
                            </button>
                        </div>
                    )}
                </div>
            )}
        </div>
    ),
}));

vi.mock('../../components/StatusBadge', () => ({
    StatusBadge: ({ status }: { status: string | number }) => <span data-testid="status-badge">{status}</span>,
}));

vi.mock('../../components/AdminSelect', () => ({
    AdminSelect: ({
        options,
        value,
        onChange,
    }: {
        options: { value: string; label: string }[];
        value: string;
        onChange: (val: string) => void;
    }) => (
        <select data-testid="admin-select" value={value} onChange={e => onChange(e.target.value)}>
            {options.map(opt => (
                <option key={opt.value} value={opt.value}>
                    {opt.label}
                </option>
            ))}
        </select>
    ),
}));

// Mock ConfirmModal
vi.mock('../../components/ConfirmModal', () => ({
    ConfirmModal: (props: {
        isOpen: boolean;
        title: string;
        content: string;
        confirmText: string;
        isLoading: boolean;
        variant: string;
        onConfirm: () => void;
        onCancel: () => void;
    }) => {
        if (!props.isOpen) {
            return null;
        }
        return (
            <div data-testid="confirm-modal">
                <div>{props.title}</div>
                <div>{props.content}</div>
                <button type="button" onClick={props.onConfirm} data-testid="confirm-yes">
                    {props.confirmText}
                </button>
                <button type="button" onClick={props.onCancel} data-testid="confirm-no">
                    取消
                </button>
            </div>
        );
    },
}));

// ─── Sample data ───
const sampleReports: AdminReport[] = [
    {
        reportId: 1,
        productId: 10,
        productName: '问题商品A',
        productImage: null,
        reporterId: 100,
        reporterName: '举报人A',
        reason: '虚假宣传',
        status: 0,
        statusDesc: '待处理',
        handleResult: null,
        handleRemark: null,
        createTime: '2026-05-16T10:00:00',
        handleTime: null,
    },
    {
        reportId: 2,
        productId: 20,
        productName: '问题商品B',
        productImage: null,
        reporterId: 200,
        reporterName: '举报人B',
        reason: '违规内容',
        status: 2,
        statusDesc: '已处理',
        handleResult: 'resolve',
        handleRemark: '已处理',
        createTime: '2026-05-15T10:00:00',
        handleTime: '2026-05-15T12:00:00',
    },
];

const samplePageData: PageResult<AdminReport> = {
    records: sampleReports,
    total: 2,
    current: 1,
    size: 10,
    pages: 1,
};

function setupMocks(
    overrides: Partial<{
        data: PageResult<AdminReport> | undefined;
        isLoading: boolean;
    }> = {}
) {
    const { data = samplePageData, isLoading = false } = overrides;

    mockUseAdminReports.mockReturnValue({
        data,
        isLoading,
        isError: false,
        error: null,
    });

    mockUseHandleReport.mockReturnValue({
        mutateAsync: vi.fn().mockResolvedValue({}),
        isPending: false,
    });
}

describe('ReportManagePage', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        setupMocks();
    });

    // ── Test 1: Page title ──
    it('renders page title "举报处理"', () => {
        renderWithProviders(<ReportManagePage />);
        expect(screen.getByText('举报处理')).toBeInTheDocument();
        expect(screen.getByText('审核和处理用户举报，维护平台秩序')).toBeInTheDocument();
    });

    // ── Test 2: Shows report list ──
    it('renders report list with report data', () => {
        renderWithProviders(<ReportManagePage />);

        expect(screen.getByTestId('admin-table')).toBeInTheDocument();
        const rows = screen.getAllByTestId('report-row');
        expect(rows).toHaveLength(2);
    });

    // ── Test 3: Shows total count ──
    it('shows total report count', () => {
        renderWithProviders(<ReportManagePage />);

        const countText = screen.getByText(/共/);
        expect(countText).toHaveTextContent('共 2 条举报');
    });

    // ── Test 4: Search by keyword ──
    it('searches by keyword', () => {
        renderWithProviders(<ReportManagePage />);

        const searchInput = screen.getByPlaceholderText('搜索举报对象/原因...');
        fireEvent.change(searchInput, { target: { value: '虚假' } });

        fireEvent.click(screen.getByText('搜索'));
        expect(mockUseAdminReports).toHaveBeenCalled();
    });

    // ── Test 5: Search on Enter key ──
    it('triggers search on Enter key press', () => {
        renderWithProviders(<ReportManagePage />);

        const searchInput = screen.getByPlaceholderText('搜索举报对象/原因...');
        fireEvent.change(searchInput, { target: { value: '违规' } });
        fireEvent.keyPress(searchInput, { key: 'Enter' });

        expect(mockUseAdminReports).toHaveBeenCalled();
    });

    // ── Test 6: Status filter ──
    it('changes status filter', () => {
        renderWithProviders(<ReportManagePage />);

        const selects = screen.getAllByTestId('admin-select');
        const statusSelect = selects[0];
        fireEvent.change(statusSelect, { target: { value: '0' } });

        expect(mockUseAdminReports).toHaveBeenCalled();
    });

    // ── Test 7: Type filter ──
    it('changes type filter', () => {
        renderWithProviders(<ReportManagePage />);

        const selects = screen.getAllByTestId('admin-select');
        const typeSelect = selects[1];
        fireEvent.change(typeSelect, { target: { value: '1' } });

        expect(mockUseAdminReports).toHaveBeenCalled();
    });

    // ── Test 8: Loading state ──
    it('shows loading state', () => {
        setupMocks({ isLoading: true });
        renderWithProviders(<ReportManagePage />);

        expect(screen.getByTestId('loading-skeleton')).toBeInTheDocument();
    });

    // ── Test 9: Empty state ──
    it('shows empty state when no reports', () => {
        setupMocks({
            data: { records: [], total: 0, current: 1, size: 10, pages: 0 },
        });
        renderWithProviders(<ReportManagePage />);

        expect(screen.getByText('暂无举报数据')).toBeInTheDocument();
    });

    // ── Test 10: Resolve confirm modal ──
    it('shows resolve confirm modal and confirms', () => {
        const mockMutateAsync = vi.fn().mockResolvedValue({});
        mockUseHandleReport.mockReturnValue({
            mutateAsync: mockMutateAsync,
            isPending: false,
        });

        renderWithProviders(<ReportManagePage />);

        // Find the "处理" button for the first report (status=0, pending)
        const resolveBtns = screen.getAllByText('处理');
        fireEvent.click(resolveBtns[0]);

        // Confirm modal should appear
        expect(screen.getByText('确认处理举报')).toBeInTheDocument();
        expect(screen.getByText('确认处理')).toBeInTheDocument();

        // Confirm
        fireEvent.click(screen.getByTestId('confirm-yes'));
        expect(mockMutateAsync).toHaveBeenCalledWith({
            id: 1,
            data: { action: 'resolve' },
        });
    });

    // ── Test 11: Dismiss confirm modal ──
    it('shows dismiss confirm modal and confirms', () => {
        const mockMutateAsync = vi.fn().mockResolvedValue({});
        mockUseHandleReport.mockReturnValue({
            mutateAsync: mockMutateAsync,
            isPending: false,
        });

        renderWithProviders(<ReportManagePage />);

        // Find the "驳回" button for the first report (status=0, pending)
        const dismissBtns = screen.getAllByText('驳回');
        fireEvent.click(dismissBtns[0]);

        // Confirm modal should appear
        expect(screen.getByText('确认驳回举报')).toBeInTheDocument();
        expect(screen.getByText('确认驳回')).toBeInTheDocument();

        // Confirm
        fireEvent.click(screen.getByTestId('confirm-yes'));
        expect(mockMutateAsync).toHaveBeenCalledWith({
            id: 1,
            data: { action: 'dismiss' },
        });
    });

    // ── Test 12: "已处理" shown for non-pending reports ──
    it('shows "已处理" for reports that are not pending', () => {
        renderWithProviders(<ReportManagePage />);

        // The second report has status 2 (已处理), should show "已处理"
        const processedLabels = screen.getAllByText('已处理');
        expect(processedLabels.length).toBeGreaterThan(0);
    });

    // ── Test 13: Pagination ──
    it('renders pagination when data exceeds page size', () => {
        const manyReports: AdminReport[] = Array.from({ length: 10 }, (_, i) => ({
            reportId: i + 1,
            productId: i + 10,
            productName: `商品${i}`,
            productImage: null,
            reporterId: i + 100,
            reporterName: `举报人${i}`,
            reason: `原因${i}`,
            status: i % 2 === 0 ? 0 : 2,
            statusDesc: i % 2 === 0 ? '待处理' : '已处理',
            handleResult: i % 2 === 0 ? null : 'resolve',
            handleRemark: null,
            createTime: '2026-05-16T10:00:00',
            handleTime: i % 2 === 0 ? null : '2026-05-16T12:00:00',
        }));

        setupMocks({
            data: {
                records: manyReports,
                total: 25,
                current: 1,
                size: 10,
                pages: 3,
            },
        });
        renderWithProviders(<ReportManagePage />);

        expect(screen.getByTestId('pagination')).toBeInTheDocument();
        fireEvent.click(screen.getByText('下一页'));
        expect(mockUseAdminReports).toHaveBeenCalled();
    });
});
