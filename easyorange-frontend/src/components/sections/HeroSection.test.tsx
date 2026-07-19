import { fireEvent, render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import HeroSection from './HeroSection';

// Mock IntersectionObserver
const mockObserve = vi.fn();
const mockDisconnect = vi.fn();
class MockIntersectionObserver {
    observe = mockObserve;
    disconnect = mockDisconnect;
    unobserve = vi.fn();
}
vi.stubGlobal('IntersectionObserver', MockIntersectionObserver);

// Mock requestAnimationFrame / cancelAnimationFrame
const rafCbs: Map<number, FrameRequestCallback> = new Map();
let rafId = 0;
vi.stubGlobal(
    'requestAnimationFrame',
    vi.fn((cb: FrameRequestCallback) => {
        rafId++;
        rafCbs.set(rafId, cb);
        return rafId;
    })
);
vi.stubGlobal(
    'cancelAnimationFrame',
    vi.fn((id: number) => {
        rafCbs.delete(id);
    })
);

const mockNavigate = vi.fn();
vi.mock('react-router-dom', () => ({
    useNavigate: () => mockNavigate,
}));

const mockUsePlatformStats = vi.fn();
vi.mock('@/hooks', () => ({
    usePlatformStats: () => mockUsePlatformStats(),
}));

// Mock Image component
vi.mock('@/components/ui/Image', () => ({
    Image: ({ alt, ...props }: { alt: string; [key: string]: unknown }) => (
        <img alt={alt} {...props} data-testid="hero-image" />
    ),
}));

// Mock window.matchMedia
Object.defineProperty(window, 'matchMedia', {
    writable: true,
    value: vi.fn().mockImplementation((query: string) => ({
        matches: false,
        media: query,
        onchange: null,
        addListener: vi.fn(),
        removeListener: vi.fn(),
        addEventListener: vi.fn(),
        removeEventListener: vi.fn(),
        dispatchEvent: vi.fn(),
    })),
});

// Mock HTMLSpanElement's getBoundingClientRect
// The HeroSection uses data-count attributes on stat elements
const originalGetBoundingClientRect = Element.prototype.getBoundingClientRect;

describe('HeroSection', () => {
    beforeEach(() => {
        vi.clearAllMocks();
        rafCbs.clear();
        rafId = 0;
        mockUsePlatformStats.mockReturnValue({
            data: { activeUsers: 1000, onlineProducts: 500, completedOrders: 300 },
        });

        Element.prototype.getBoundingClientRect = vi.fn(() => ({
            width: 100,
            height: 100,
            top: 0,
            left: 0,
            bottom: 100,
            right: 100,
            x: 0,
            y: 0,
            toJSON: () => ({}),
        }));
    });

    afterEach(() => {
        Element.prototype.getBoundingClientRect = originalGetBoundingClientRect;
    });

    it('renders the brand name', () => {
        render(<HeroSection />);
        expect(screen.getByText('EasyOrange')).toBeInTheDocument();
    });

    it('renders the main title', () => {
        render(<HeroSection />);
        const rangChars = screen.getAllByText('让');
        expect(rangChars.length).toBe(1);
        expect(screen.getByText('每')).toBeInTheDocument();
        expect(screen.getByText('一')).toBeInTheDocument();
        expect(screen.getByText('份')).toBeInTheDocument();
        expect(screen.getByText('资')).toBeInTheDocument();
        expect(screen.getByText('产')).toBeInTheDocument();
    });

    it('renders the subtitle', () => {
        render(<HeroSection />);
        expect(screen.getByText('LLM × DDD · 业务聚焦核心流程，把复杂度留给架构与 AI 工程化')).toBeInTheDocument();
    });

    it('renders search input', () => {
        render(<HeroSection />);
        const searchInput = screen.getByPlaceholderText('搜索你想要的资产...');
        expect(searchInput).toBeInTheDocument();
    });

    it('renders search button', () => {
        render(<HeroSection />);
        expect(screen.getByText('搜索')).toBeInTheDocument();
    });

    it('renders hot search tags', () => {
        render(<HeroSection />);
        expect(screen.getByText('热门搜索:')).toBeInTheDocument();
        expect(screen.getByText('教材')).toBeInTheDocument();
        expect(screen.getByText('视频会员')).toBeInTheDocument();
        const designTags = screen.getAllByText('设计素材');
        expect(designTags.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('考研资料')).toBeInTheDocument();
    });

    it('navigates to search on hot tag click', () => {
        render(<HeroSection />);
        fireEvent.click(screen.getByText('教材'));
        expect(mockNavigate).toHaveBeenCalledWith('/search?keyword=%E6%95%99%E6%9D%90');
    });

    it('navigates to search on form submit', () => {
        render(<HeroSection />);
        const input = screen.getByPlaceholderText('搜索你想要的资产...');
        fireEvent.change(input, { target: { value: 'macbook' } });
        fireEvent.submit(screen.getByRole('button', { name: '搜索' }).closest('form') as HTMLFormElement);
        expect(mockNavigate).toHaveBeenCalledWith('/search?keyword=macbook');
    });

    it('does not navigate on empty search', () => {
        render(<HeroSection />);
        const form = screen.getByRole('button', { name: '搜索' }).closest('form') as HTMLFormElement;
        fireEvent.submit(form);
        expect(mockNavigate).not.toHaveBeenCalled();
    });

    it('renders AI entry button', () => {
        render(<HeroSection />);
        expect(screen.getByText('AI 工程化')).toBeInTheDocument();
        expect(screen.getByText('拍照估值 · 智能写描述 · 一键发布')).toBeInTheDocument();
    });

    it('renders platform stats with data-count attributes', () => {
        render(<HeroSection />);
        expect(screen.getByText('活跃用户')).toBeInTheDocument();
        expect(screen.getByText('在管资产')).toBeInTheDocument();
        expect(screen.getByText('AI 成交')).toBeInTheDocument();
    });

    it('renders stat values with data attributes', () => {
        render(<HeroSection />);
        const statEls = document.querySelectorAll('[data-count]');
        expect(statEls.length).toBe(3);
        expect(statEls[0].getAttribute('data-count')).toBe('5280');
        expect(statEls[1].getAttribute('data-count')).toBe('3560');
        expect(statEls[2].getAttribute('data-count')).toBe('2180');
    });

    it('renders scroll indicator', () => {
        render(<HeroSection />);
        expect(screen.getByText('向下滚动探索更多')).toBeInTheDocument();
    });

    it('renders product preview with image', () => {
        render(<HeroSection />);
        expect(screen.getByText('MacBook Pro 14" M3 Pro')).toBeInTheDocument();
        expect(screen.getByText('¥12,999')).toBeInTheDocument();
        expect(screen.getByText('原价 ¥16,999')).toBeInTheDocument();
    });

    it('creates IntersectionObserver for counter animation', () => {
        render(<HeroSection />);
        expect(mockObserve).toHaveBeenCalled();
    });

    it('renders floating category cards', () => {
        render(<HeroSection />);
        expect(screen.getByText('教材资料')).toBeInTheDocument();
        const electronicCards = screen.getAllByText('电子产品');
        expect(electronicCards.length).toBeGreaterThanOrEqual(1);
        expect(screen.getByText('交通工具')).toBeInTheDocument();
    });

    it('renders hero section element', () => {
        const { container } = render(<HeroSection />);
        expect(container.querySelector('.hero-section')).toBeInTheDocument();
    });

    it('disconnects observer on unmount', () => {
        const { unmount } = render(<HeroSection />);
        unmount();
        expect(mockDisconnect).toHaveBeenCalled();
    });
});
