import { useCallback, useMemo, useState } from 'react';
import placeholderImage from '@/assets/placeholder.png';

interface ImageProps extends Omit<React.ImgHTMLAttributes<HTMLImageElement>, 'src' | 'srcSet'> {
    src: string | undefined;
    fallback?: string;
    placeholder?: 'blur' | 'skeleton' | 'none';
    containerClassName?: string;
    fetchPriority?: 'high' | 'low' | 'auto';
    widths?: number[];
    format?: 'webp' | 'jpeg' | 'png' | 'original';
    quality?: number;
    lazy?: boolean;
}

const PLACEHOLDER_BASE_STYLE: React.CSSProperties = {
    position: 'absolute',
    inset: 0,
    backgroundColor: 'var(--color-surface, #f3f4f6)',
};

const SKELETON_STYLE: React.CSSProperties = {
    ...PLACEHOLDER_BASE_STYLE,
    zIndex: 1,
    background: 'linear-gradient(90deg, #f0f0f0 25%, #e0e0e0 50%, #f0f0f0 75%)',
    backgroundSize: '200% 100%',
    animation: 'shimmer 1.5s infinite',
};

const BLUR_STYLE: React.CSSProperties = {
    ...PLACEHOLDER_BASE_STYLE,
    backdropFilter: 'blur(20px)',
    WebkitBackdropFilter: 'blur(20px)',
};

const DEFAULT_WIDTHS = [150, 300, 600, 1200];

let cachedWebPSupport: boolean | null = null;

function getWebPSupport(): boolean {
    if (cachedWebPSupport !== null) {
        return cachedWebPSupport;
    }
    if (typeof document === 'undefined') {
        return false;
    }
    const canvas = document.createElement('canvas');
    canvas.width = 1;
    canvas.height = 1;
    const dataUrl = canvas.toDataURL('image/webp');
    cachedWebPSupport = dataUrl.startsWith('data:image/webp');
    return cachedWebPSupport;
}

function extractFileId(src: string | undefined): string | null {
    if (!src) {
        return null;
    }

    const match = src.match(/\/api\/file\/([^/]+)/);
    if (match) {
        return match[1];
    }

    if (/^\d+$/.test(src)) {
        return src;
    }

    return null;
}

function resolveImageUrl(src: string | undefined): string {
    if (!src) {
        return '';
    }

    const lowerSrc = src.toLowerCase().trim();
    const dangerousProtocols = ['javascript:', 'vbscript:', 'file:'];
    if (dangerousProtocols.some(p => lowerSrc.startsWith(p))) {
        return '';
    }

    if (src.startsWith('data:')) {
        if (!lowerSrc.startsWith('data:image/')) {
            return '';
        }
        return src;
    }
    if (src.startsWith('http://') || src.startsWith('https://')) {
        return src;
    }
    if (src.startsWith('/api/')) {
        return src;
    }
    if (src.startsWith('/')) {
        return src;
    }
    return `/api/file/${encodeURIComponent(src)}`;
}

function buildResponsiveUrl(fileId: string, width: number, format: string, quality: number): string {
    return `/api/file/${fileId}/responsive?w=${width}&format=${format}&q=${quality}`;
}

function buildViewUrl(fileId: string, width?: number, height?: number, format?: string, quality?: number): string {
    const params = new URLSearchParams();
    if (width) {
        params.set('w', String(width));
    }
    if (height) {
        params.set('h', String(height));
    }
    if (format) {
        params.set('format', format);
    }
    if (quality) {
        params.set('q', String(quality));
    }
    const queryString = params.toString();
    return `/api/file/${fileId}/view${queryString ? `?${queryString}` : ''}`;
}

function buildThumbnailUrl(fileId: string, size: number): string {
    return `/api/file/${fileId}/thumbnail?size=${size}`;
}

function useSupportsWebP(): boolean {
    const [supports] = useState(() => getWebPSupport());
    return supports;
}

export function Image({
    src,
    alt,
    fallback = placeholderImage,
    placeholder = 'blur',
    className,
    containerClassName,
    style,
    fetchPriority,
    onLoad,
    widths = DEFAULT_WIDTHS,
    format = 'webp',
    quality = 80,
    lazy = true,
    ...props
}: ImageProps) {
    const [error, setError] = useState(false);
    const [loaded, setLoaded] = useState(false);

    const supportsWebP = useSupportsWebP();
    const resolvedSrc = resolveImageUrl(src);
    const fileId = extractFileId(src);

    const actualFormat = useMemo(() => {
        if (format === 'original') {
            return 'original';
        }
        if (format === 'webp' && !supportsWebP) {
            return 'jpeg';
        }
        return format;
    }, [format, supportsWebP]);

    const { mainSrc, srcSet, sizes } = useMemo(() => {
        if (!fileId || actualFormat === 'original') {
            return { mainSrc: resolvedSrc, srcSet: undefined, sizes: undefined };
        }

        const srcSetParts = widths.map(w => `${buildResponsiveUrl(fileId, w, actualFormat, quality)} ${w}w`);

        const sizesAttr = `(max-width: ${widths[0]}px) ${widths[0]}px, ${widths
            .slice(1)
            .map((w, i) => `(max-width: ${w}px) ${widths[i]}px`)
            .join(', ')}, ${widths[widths.length - 1]}px`;

        return {
            mainSrc: buildResponsiveUrl(fileId, widths[widths.length - 1], actualFormat, quality),
            srcSet: srcSetParts.join(', '),
            sizes: sizesAttr,
        };
    }, [fileId, actualFormat, quality, widths, resolvedSrc]);

    const handleLoad = useCallback(
        (e: React.SyntheticEvent<HTMLImageElement>) => {
            setLoaded(true);
            setError(false);
            onLoad?.(e);
        },
        [onLoad]
    );

    const finalSrc = error ? fallback : mainSrc;
    const showPlaceholder = placeholder !== 'none' && !loaded && !error;

    const placeholderStyle = placeholder === 'skeleton' ? SKELETON_STYLE : BLUR_STYLE;

    const imgStyle = useMemo<React.CSSProperties>(
        () => ({
            display: 'block',
            width: '100%',
            height: '100%',
            objectFit: style?.objectFit || 'cover',
            transition: 'opacity 0.3s ease',
            ...style,
            opacity: loaded ? 1 : 0,
        }),
        [style, loaded]
    );

    return (
        <div
            className={containerClassName}
            style={{
                position: 'relative',
                overflow: 'hidden',
                width: '100%',
                height: '100%',
            }}
        >
            {showPlaceholder && <div style={placeholderStyle} />}
            <img
                src={finalSrc}
                srcSet={srcSet}
                sizes={sizes}
                alt={alt}
                className={className}
                loading={lazy ? 'lazy' : 'eager'}
                decoding="async"
                fetchPriority={fetchPriority}
                onError={() => setError(true)}
                onLoad={handleLoad}
                style={imgStyle}
                {...props}
            />
        </div>
    );
}

export function preloadImage(
    src: string,
    options?: {
        width?: number;
        format?: string;
        quality?: number;
    }
): Promise<void> {
    return new Promise((resolve, reject) => {
        const img = new window.Image();
        img.onload = () => resolve();
        img.onerror = reject;

        const fileId = extractFileId(src);
        if (fileId && options) {
            img.src = buildViewUrl(fileId, options.width, undefined, options.format, options.quality);
        } else {
            img.src = resolveImageUrl(src);
        }
    });
}

export function preloadImages(
    sources: string[],
    options?: {
        width?: number;
        format?: string;
        quality?: number;
    }
    // biome-ignore lint/suspicious/noConfusingVoidType: side-effect preload returns void[]
): Promise<void[]> {
    return Promise.all(sources.map(src => preloadImage(src, options)));
}

export { buildResponsiveUrl, buildThumbnailUrl, buildViewUrl };
