import { screen } from '@testing-library/react';
import { HttpResponse, http } from 'msw';
import { afterEach, describe, expect, it, vi } from 'vitest';
import { server } from '@/testUtils/mocks/server';
import { renderWithProviders } from '@/testUtils/renderWithProviders';
import { NotificationBell } from './NotificationBell';

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
    const actual = await vi.importActual('react-router-dom');
    return {
        ...actual,
        useNavigate: () => mockNavigate,
    };
});

afterEach(() => {
    server.resetHandlers();
    mockNavigate.mockClear();
});

describe('NotificationBell', () => {
    it('renders bell button', () => {
        server.use(
            http.get('/api/messages/unread-count', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { systemCount: 0, totalCount: 0 },
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<NotificationBell />);
        expect(screen.getByLabelText('通知')).toBeInTheDocument();
    });

    it('shows badge with unread count', async () => {
        server.use(
            http.get('/api/messages/unread-count', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { systemCount: 3, totalCount: 3 },
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<NotificationBell />);
        const badge = await screen.findByText('3');
        expect(badge).toBeInTheDocument();
    });

    it('does not show badge when count is 0', async () => {
        server.use(
            http.get('/api/messages/unread-count', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { systemCount: 0, totalCount: 0 },
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<NotificationBell />);
        // Wait for query to resolve
        await new Promise(r => setTimeout(r, 100));
        expect(screen.queryByText('0')).not.toBeInTheDocument();
    });

    it('shows 99+ when count exceeds 99', async () => {
        server.use(
            http.get('/api/messages/unread-count', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { systemCount: 150, totalCount: 150 },
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<NotificationBell />);
        const badge = await screen.findByText('99+');
        expect(badge).toBeInTheDocument();
    });

    it('navigates to notifications on click', async () => {
        server.use(
            http.get('/api/messages/unread-count', () => {
                return HttpResponse.json({
                    code: 'A0000',
                    message: 'success',
                    data: { systemCount: 0, totalCount: 0 },
                    timestamp: Date.now(),
                });
            })
        );

        renderWithProviders(<NotificationBell />);
        screen.getByLabelText('通知').click();
        expect(mockNavigate).toHaveBeenCalledWith('/notifications');
    });
});
