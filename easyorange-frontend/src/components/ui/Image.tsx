import { useState } from 'react';

interface ImageProps extends React.ImgHTMLAttributes<HTMLImageElement> {
  fallback?: string;
}

export function Image({ src, alt, fallback = '/placeholder.png', className, ...props }: ImageProps) {
  const [error, setError] = useState(false);

  return (
    <img
      src={error ? fallback : src}
      alt={alt}
      className={className}
      loading="lazy"
      onError={() => setError(true)}
      {...props}
    />
  );
}
