import { useState, useCallback } from 'react';
import { ChevronLeft, ChevronRight, Heart, Share2 } from 'lucide-react';
import { Image, preloadImages, buildThumbnailUrl } from '@/components/ui/Image';
import placeholderImage from '@/assets/placeholder.png';

interface ProductGalleryProps {
  images: string[];
  isFavorited: boolean;
  isFavoriteLoading: boolean;
  isSold: boolean;
  onFavoriteToggle: () => void;
  onShare: () => void;
}

export function ProductGallery({
  images,
  isFavorited,
  isFavoriteLoading,
  isSold,
  onFavoriteToggle,
  onShare,
}: ProductGalleryProps) {
  const [currentImageIndex, setCurrentImageIndex] = useState(0);
  const [imageLoaded, setImageLoaded] = useState(false);

  const productImages = images.length > 0 ? images : [placeholderImage];

  const preloadAdjacentImages = useCallback((centerIdx: number, allImages: string[]) => {
    if (allImages.length <= 1) return;
    const prevIdx = (centerIdx - 1 + allImages.length) % allImages.length;
    const nextIdx = (centerIdx + 1) % allImages.length;
    preloadImages([allImages[prevIdx], allImages[nextIdx]], { width: 600, format: 'webp', quality: 80 }).catch(() => {});
  }, []);

  const handlePrevImage = () => {
    const prevIndex = (currentImageIndex - 1 + productImages.length) % productImages.length;
    setCurrentImageIndex(prevIndex);
    preloadAdjacentImages(prevIndex, productImages);
  };

  const handleNextImage = () => {
    const nextIndex = (currentImageIndex + 1) % productImages.length;
    setCurrentImageIndex(nextIndex);
    preloadAdjacentImages(nextIndex, productImages);
  };

  return (
    <div className="pdp-gallery">
      <div className="pdp-gallery-main">
        <div className={`pdp-gallery-image-wrapper ${imageLoaded ? 'loaded' : ''}`}>
          {(() => {
            const fileIdMatch = productImages[currentImageIndex]?.match(/\/api\/file\/([^/]+)/);
            const fileId = fileIdMatch?.[1];
            const thumbSrc = fileId ? buildThumbnailUrl(fileId, 400) : productImages[currentImageIndex];
            return (
              <>
                {!imageLoaded && (
                  <Image
                    src={thumbSrc}
                    alt="缩略预览"
                    className="pdp-gallery-image"
                    loading="eager"
                    fetchPriority="high"
                    placeholder="blur"
                    style={{ width: '100%', height: '100%', objectFit: 'contain', position: 'absolute', inset: 0 }}
                  />
                )}
                <Image
                  src={productImages[currentImageIndex]}
                  alt={`图片 ${currentImageIndex + 1}`}
                  className="pdp-gallery-image"
                  loading="eager"
                  fetchPriority="high"
                  placeholder={imageLoaded ? 'none' : 'none'}
                  onLoad={() => setImageLoaded(true)}
                  style={{
                    width: '100%',
                    height: '100%',
                    objectFit: 'contain',
                    position: imageLoaded ? 'relative' : 'absolute',
                    inset: 0,
                    zIndex: imageLoaded ? 1 : 0,
                  }}
                />
              </>
            );
          })()}
        </div>

        {isSold && (
          <div className="pdp-sold-overlay">
            <span className="pdp-sold-badge">已售出</span>
          </div>
        )}

        {productImages.length > 1 && (
          <>
            <button onClick={handlePrevImage} className="pdp-gallery-nav pdp-gallery-prev">
              <ChevronLeft size={20} />
            </button>
            <button onClick={handleNextImage} className="pdp-gallery-nav pdp-gallery-next">
              <ChevronRight size={20} />
            </button>
            <div className="pdp-gallery-counter">
              {currentImageIndex + 1} / {productImages.length}
            </div>
          </>
        )}

        <div className="pdp-gallery-actions">
          <button
            className={`pdp-action-fab ${isFavorited ? 'favorited' : ''}`}
            onClick={onFavoriteToggle}
            disabled={isFavoriteLoading}
          >
            <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} />
          </button>
          <button className="pdp-action-fab" onClick={onShare}>
            <Share2 size={18} />
          </button>
        </div>
      </div>

      {productImages.length > 1 && (
        <div className="pdp-gallery-thumbs">
          {productImages.map((img, idx) => (
            <button
              key={idx}
              onClick={() => setCurrentImageIndex(idx)}
              className={`pdp-thumb ${idx === currentImageIndex ? 'active' : ''}`}
            >
              <Image
                src={img}
                alt={`缩略图 ${idx + 1}`}
                loading="lazy"
                placeholder="skeleton"
                style={{ width: '100%', height: '100%', objectFit: 'cover' }}
              />
              <div className="pdp-thumb-indicator" />
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
