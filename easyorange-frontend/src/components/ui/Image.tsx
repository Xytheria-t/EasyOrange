import { useState, useRef, useEffect } from 'react';

interface ImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallback?: string;
  placeholder?: 'blur' | 'skeleton' | 'none';
}

export function Image({
  src,
  alt,
  fallback = '/placeholder.png',
  placeholder = 'blur',
  className,
  style,
  loading = 'lazy',
  fetchPriority,
  ...props
}: ImageProps) {
  const [error, setError] = useState(false);
  const [loaded, setLoaded] = useState(false);
  const imgRef = useRef<HTMLImageElement>(null);

  const currentSrc = error ? fallback : src;

  useEffect(() => {
    if (imgRef.current?.complete) {
      setLoaded(true);
    }
  }, [currentSrc]);

  const showPlaceholder = placeholder !== 'none' && !loaded && !error;

  const placeholderStyle: React.CSSProperties =
    placeholder === 'blur'
      ? {
          backgroundColor: 'var(--color-surface, #f3f4f6)',
          backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
        }
      : {
          backgroundColor: 'var(--color-surface, #f3f4f6)',
        };

  return (
    <div
      className={className}
      style={{
        position: 'relative',
        overflow: 'hidden',
        ...style,
      }}
    >
      {showPlaceholder && (
        <div
          style={{
            position: 'absolute',
            inset: 0,
            zIndex: 1,
            ...placeholderStyle,
          }}
        />
      )}
      <img
        ref={imgRef}
        src={currentSrc}
        alt={alt}
        loading={loading}
        decoding="async"
        fetchPriority={fetchPriority}
        onError={() => setError(true)}
        onLoad={() => setLoaded(true)}
        style={{
          display: 'block',
          width: '100%',
          height: '100%',
          objectFit: 'inherit',
          opacity: loaded ? 1 : 0,
          transition: 'opacity 0.3s ease',
        }}
        {...props}
      />
    </div>
  );
}
