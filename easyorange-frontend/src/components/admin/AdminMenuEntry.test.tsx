import { describe, it, expect, vi } from 'vitest';
import { render, screen } from '@testing-library/react';
import userEvent from '@testing-library/user-event';
import { MemoryRouter } from 'react-router-dom';
import { AdminMenuEntry } from './AdminMenuEntry';

// Mock useAdminGuard
const mockUseAdminGuard = vi.fn();
vi.mock('@/admin/hooks/useAdminGuard', () => ({
  useAdminGuard: () => mockUseAdminGuard(),
}));

const mockNavigate = vi.fn();
vi.mock('react-router-dom', async () => {
  const actual = await vi.importActual('react-router-dom');
  return {
    ...actual,
    useNavigate: () => mockNavigate,
  };
});

describe('AdminMenuEntry', () => {
  beforeEach(() => {
    vi.clearAllMocks();
  });

  it('renders nothing when isAdmin is false', () => {
    mockUseAdminGuard.mockReturnValue({ isAdmin: false });
    const { container } = render(
      <MemoryRouter>
        <AdminMenuEntry />
      </MemoryRouter>,
    );
    expect(container.innerHTML).toBe('');
  });

  it('renders admin button when isAdmin is true', () => {
    mockUseAdminGuard.mockReturnValue({ isAdmin: true });
    render(
      <MemoryRouter>
        <AdminMenuEntry />
      </MemoryRouter>,
    );
    expect(screen.getByLabelText('进入后台管理')).toBeInTheDocument();
    expect(screen.getByText('后台管理')).toBeInTheDocument();
  });

  it('navigates to /admin on click', async () => {
    mockUseAdminGuard.mockReturnValue({ isAdmin: true });
    render(
      <MemoryRouter>
        <AdminMenuEntry />
      </MemoryRouter>,
    );
    await userEvent.click(screen.getByLabelText('进入后台管理'));
    expect(mockNavigate).toHaveBeenCalledWith('/admin');
  });
});
