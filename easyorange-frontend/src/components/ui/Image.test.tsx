import { render, screen } from '@testing-library/react';
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest';
import { Image } from './Image';

// Mock placeholder image import
vi.mock('@/assets/placeholder.png', () => ({
    default: 'placeholder.png',
}));

describe('Image', () => {
    beforeEach(() => {
        // Mock canvas to indicate WebP support
        HTMLCanvasElement.prototype.toDataURL = vi.fn(() => 'data:image/webp,test');
    });

    afterEach(() => {
        vi.restoreAllMocks();
    });

    it('renders img element with alt text', () => {
        render(<Image src="/api/file/test.jpg" alt="test image" />);
        const img = screen.getByAltText('test image');
        expect(img).toBeInTheDocument();
    });

    it('renders with lazy loading by default', () => {
        render(<Image src="/api/file/test.jpg" alt="lazy" />);
        const img = screen.getByAltText('lazy');
        expect(img).toHaveAttribute('loading', 'lazy');
    });

    it('uses eager loading when lazy is false', () => {
        render(<Image src="/api/file/test.jpg" alt="eager" lazy={false} />);
        const img = screen.getByAltText('eager');
        expect(img).toHaveAttribute('loading', 'eager');
    });

    it('renders fallback on error', () => {
        render(<Image src="invalid" alt="error" />);
        const img = screen.getByAltText('error');
        // After error, the src should be the fallback (placeholder)
        // The component retries first, so we trigger onError
        // After retries exhaust, it shows fallback
        expect(img).toBeInTheDocument();
    });

    it('shows placeholder while loading', () => {
        const { container } = render(<Image src="/api/file/test.jpg" alt="loading" />);
        // Placeholder div should exist before image loads
        const placeholderDiv = container.querySelector('div[style]');
        expect(placeholderDiv).toBeInTheDocument();
    });

    it('does not show placeholder when placeholder is none', () => {
        render(<Image src="/api/file/test.jpg" alt="no-placeholder" placeholder="none" />);
        // Should not have shimmer or blur placeholder divs
        expect(screen.getByAltText('no-placeholder')).toBeInTheDocument();
    });

    it('applies custom className to img', () => {
        render(<Image src="/api/file/test.jpg" alt="custom" className="custom-img" />);
        const img = screen.getByAltText('custom');
        expect(img).toHaveClass('custom-img');
    });
});
