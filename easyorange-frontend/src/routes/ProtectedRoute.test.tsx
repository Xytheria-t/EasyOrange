import { act, render, screen } from '@testing-library/react';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { ProtectedRoute } from './ProtectedRoute';

const mockGetStoredToken = vi.hoisted(() => vi.fn());
const mockRestoreSession = vi.hoisted(() => vi.fn());

vi.mock('@/features/auth/session', () => ({
    getStoredToken: mockGetStoredToken,
    restoreSession: mockRestoreSession,
}));

function renderGuard() {
    return render(
        <MemoryRouter initialEntries={['/favorites']}>
            <Routes>
                <Route
                    path="/favorites"
                    element={
                        <ProtectedRoute>
                            <div>收藏内容</div>
                        </ProtectedRoute>
                    }
                />
                <Route path="/login" element={<div>登录页</div>} />
            </Routes>
        </MemoryRouter>
    );
}

describe('ProtectedRoute', () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it('会话恢复完成后才渲染受保护内容', async () => {
        mockGetStoredToken.mockReturnValue(null);
        let finishRestore: () => void = () => {};
        mockRestoreSession.mockReturnValue(
            new Promise<void>(resolve => {
                finishRestore = resolve;
            })
        );

        renderGuard();

        // 恢复期间既不跳登录页也不放行内容
        expect(screen.queryByText('收藏内容')).not.toBeInTheDocument();
        expect(screen.queryByText('登录页')).not.toBeInTheDocument();

        mockGetStoredToken.mockReturnValue('access-token');
        await act(async () => {
            finishRestore();
        });

        expect(screen.getByText('收藏内容')).toBeInTheDocument();
    });

    it('恢复后仍无 token 才跳登录页并带上回跳地址', async () => {
        mockGetStoredToken.mockReturnValue(null);
        mockRestoreSession.mockResolvedValue(undefined);

        renderGuard();

        expect(await screen.findByText('登录页')).toBeInTheDocument();
    });

    it('已有 token 时不触发会话恢复', () => {
        mockGetStoredToken.mockReturnValue('access-token');

        renderGuard();

        expect(screen.getByText('收藏内容')).toBeInTheDocument();
        expect(mockRestoreSession).not.toHaveBeenCalled();
    });
});
