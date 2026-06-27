import { useRef, useState, useCallback, memo } from 'react'
import { useNavigate } from 'react-router-dom'
import { Heart, MessageCircle, Eye, MapPin, Clock, Sparkles } from 'lucide-react'
import type { Product } from '@/types'
import { CONDITION_LABEL_MAP } from '@/constants'
import { formatPrice, formatRelativeTime } from '@/utils'
import { Image } from '@/components/ui/Image'
import { AiTag } from './AiTag'
import placeholderImage from '@/assets/placeholder.png'
import '../../pages/products/products-premium.css'

interface ProductCardProps {
  product: Product
  onFavorite?: (id: string, isFavorited: boolean) => void;
  isFavorited?: boolean
  style?: React.CSSProperties
  index?: number
  aiTags?: string[]
}

export const ProductCard = memo(({
  product,
  onFavorite,
  isFavorited = false,
  style,
  index = 0,
  aiTags,
}: ProductCardProps) => {
  const navigate = useNavigate()
  const cardRef = useRef<HTMLDivElement>(null)
  const [isHovered, setIsHovered] = useState(false)
  const [imageLoaded, setImageLoaded] = useState(false)

  const imageUrl = product.images?.[0] || placeholderImage
  const secondaryImageUrl = product.images?.[1] || null
  const conditionLabel = CONDITION_LABEL_MAP[product.condition] || product.condition
  const hasDiscount = product.originalPrice != null && product.originalPrice > product.price
  const discountPercent = hasDiscount
    ? Math.round((1 - product.price / product.originalPrice!) * 100)
    : 0
  const isHot = product.isHot || (product.views != null && product.views > 200)
  const quickLocation = product.location?.trim() || '校内面交'
  const sellerName = product.sellerName || '匿名用户'
  const hasDescription = !!product.description && product.description.trim().length > 0

  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    onFavorite?.(product.id, !isFavorited)
  }

  const handleMouseMove = useCallback((e: React.MouseEvent<HTMLDivElement>) => {
    if (!cardRef.current) {return}

    const rect = cardRef.current.getBoundingClientRect()
    const x = e.clientX - rect.left
    const y = e.clientY - rect.top
    const centerX = rect.width / 2
    const centerY = rect.height / 2
    const rotateX = (y - centerY) / 28
    const rotateY = (centerX - x) / 28

    cardRef.current.style.transform = `perspective(1000px) rotateX(${rotateX}deg) rotateY(${rotateY}deg) translateZ(12px) scale(1.02)`
  }, [])

  const handleMouseLeave = useCallback(() => {
    if (cardRef.current) {
      cardRef.current.style.transform = 'perspective(1000px) rotateX(0deg) rotateY(0deg) translateZ(0px) scale(1)'
      cardRef.current.dataset.tiltActive = 'false'
    }
    setIsHovered(false)
  }, [])

  const handleMouseEnter = useCallback(() => {
    if (cardRef.current) {
      cardRef.current.dataset.tiltActive = 'true'
    }
    setIsHovered(true)
  }, [])

  const handleButtonMouseMove = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    const btn = e.currentTarget
    const rect = btn.getBoundingClientRect()
    const x = e.clientX - rect.left - rect.width / 2
    const y = e.clientY - rect.top - rect.height / 2
    const maxDist = 6
    const dist = Math.min(Math.sqrt(x * x + y * y), maxDist)
    const angle = Math.atan2(y, x)
    const tx = Math.cos(angle) * dist
    const ty = Math.sin(angle) * dist
    btn.style.transform = `translate(${tx}px, ${ty}px) scale(1.12)`
  }, [])

  const handleButtonMouseLeave = useCallback((e: React.MouseEvent<HTMLButtonElement>) => {
    e.currentTarget.style.transform = ''
  }, [])

  // Staggered entrance animation delay
  const entranceDelay = index * 80

  const handleCardClick = (e: React.MouseEvent) => {
    if ((e.target as HTMLElement).closest('button')) {return}
    navigate(`/products/${product.id}`)
  }

  const handleCardKeyDown = (e: React.KeyboardEvent) => {
    if (e.key === 'Enter' && !(e.target as HTMLElement).closest('button')) {
      navigate(`/products/${product.id}`)
    }
  }

  return (
    <div
      ref={cardRef}
      className="product-card-premium"
      style={{
        ...style,
        animationDelay: `${entranceDelay}ms`,
      }}
      onClick={handleCardClick}
      onMouseMove={handleMouseMove}
      onMouseLeave={handleMouseLeave}
      onMouseEnter={handleMouseEnter}
      role="button"
      tabIndex={0}
      onKeyDown={handleCardKeyDown}
      aria-label={`商品：${product.title}`}
    >
      {/* Gradient border glow */}
      <div className="product-card-border-glow" />

      {/* Floating glow effect */}
      <div className="product-card-glow" />

      {/* Shimmer overlay on hover */}
      <div className={`product-card-shimmer ${isHovered ? 'active' : ''}`} />

      <figure className="product-image-premium">
        {/* Image container with 3D depth */}
        <div className="product-image-3d-container">
          <Image
            src={imageUrl}
            alt={product.title}
            loading="lazy"
            placeholder="blur"
            className={`product-image-img primary-img ${imageLoaded ? 'loaded' : ''} ${isHovered && !secondaryImageUrl ? 'hovered' : ''}`}
            style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            onLoad={() => setImageLoaded(true)}
          />

          {/* Secondary image for dual-image crossfade */}
          {secondaryImageUrl && (
            <Image
              src={secondaryImageUrl}
              alt={`${product.title} - 第二张图`}
              loading="lazy"
              placeholder="blur"
              className={`product-image-img secondary-img ${isHovered ? 'visible' : ''}`}
              style={{ width: '100%', height: '100%', objectFit: 'cover' }}
            />
          )}

          {/* Image reflection/shine effect */}
          <div className={`product-image-shine ${isHovered ? 'active' : ''}`} />

          {/* Depth shadow overlay */}
          <div className="product-image-depth" />
        </div>

        {/* Badges - floating with glass effect */}
        <div className="product-badges-premium">
          <span className="badge-premium badge-condition-premium">
            <Sparkles size={11} strokeWidth={2.5} />
            {conditionLabel}
          </span>
          {hasDiscount && (
            <span className="badge-premium badge-discount-premium">
              -{discountPercent}%
            </span>
          )}
          {isHot && (
            <span className="badge-premium badge-hot-premium">
              <span className="hot-pulse" />
              热门
            </span>
          )}
          {aiTags?.map(tag => (
            <AiTag key={tag} tag={tag} />
          ))}
        </div>

        {/* Quick meta pills */}
        <div className="product-quick-meta-premium">
          <span className="product-quick-pill-premium">
            <MapPin size={12} strokeWidth={2.5} /> {quickLocation}
          </span>
          {product.createTime && (
            <span className="product-quick-pill-premium">
              <Clock size={12} strokeWidth={2.5} /> {formatRelativeTime(product.createTime)}
            </span>
          )}
        </div>

        {/* Action buttons - slide in from right with magnetic pull */}
        <div className={`product-actions-premium ${isHovered ? 'visible' : ''}`}>
          <button
            className={`action-icon-premium favorite-btn-premium ${isFavorited ? 'favorited' : ''}`}
            onClick={handleFavoriteClick}
            onMouseMove={handleButtonMouseMove}
            onMouseLeave={handleButtonMouseLeave}
            aria-label={isFavorited ? '取消收藏' : '收藏'}
            aria-pressed={isFavorited}
          >
            <Heart size={17} fill={isFavorited ? 'currentColor' : 'none'} strokeWidth={2} />
          </button>
          {product.sellerId && (
            <button
              className="action-icon-premium contact-btn-premium"
              onClick={(e) => { e.stopPropagation() }}
              onMouseMove={handleButtonMouseMove}
              onMouseLeave={handleButtonMouseLeave}
              aria-label="联系资产方"
            >
              <MessageCircle size={17} strokeWidth={2} />
            </button>
          )}
          <button
            className="action-icon-premium view-btn-premium"
            onClick={(e) => { e.stopPropagation(); navigate(`/products/${product.id}`) }}
            onMouseMove={handleButtonMouseMove}
            onMouseLeave={handleButtonMouseLeave}
            aria-label="查看详情"
          >
            <Eye size={17} strokeWidth={2} />
          </button>
        </div>

        {/* Price tag floating on image */}
        <div className={`product-image-price-tag ${isHovered ? 'visible' : ''}`}>
          <span className="price-tag-current">¥{formatPrice(product.price)}</span>
          {hasDiscount && (
            <span className="price-tag-original">¥{formatPrice(product.originalPrice!)}</span>
          )}
        </div>
      </figure>

      {/* Product info section */}
      <div className="product-info-premium">
        <div className="product-eyebrow-premium">
          {(product.category || product.categoryName) && (
            <span className="product-category-premium">
              {product.category || product.categoryName}
            </span>
          )}
          <span className={`product-signal-premium ${isHot ? 'is-hot' : ''}`}>
            {product.favorites != null && product.favorites > 0
              ? `${product.favorites}人收藏`
              : isHot
              ? 'AI 热度精选'
              : 'AI 托管中'}
          </span>
        </div>

        <h3 className="product-title-premium">{product.title}</h3>

        {/* Description expand on hover */}
        {hasDescription && (
          <div className={`product-desc-expand ${isHovered ? 'expanded' : ''}`}>
            <p>{product.description!.slice(0, 60)}{product.description!.length > 60 ? '...' : ''}</p>
          </div>
        )}

        <div className="product-stat-row-premium">
          <span className="product-info-chip-premium">
            <Eye size={12} strokeWidth={2.5} /> {product.views || 0} 浏览
          </span>
          {product.favorites != null && product.favorites > 0 && (
            <span className="product-info-chip-premium">
              <Heart size={12} strokeWidth={2.5} /> {product.favorites} 收藏
            </span>
          )}
        </div>

        <div className="product-footer-premium">
          <div className="product-price-premium">
            <div className="price-accent-bar" />
            <div className="product-price-row-premium">
              <span className="price-current-premium">¥{formatPrice(product.price)}</span>
              {hasDiscount && (
                <span className="price-original-premium">¥{formatPrice(product.originalPrice!)}</span>
              )}
            </div>
            {hasDiscount && (
              <span className="price-note-premium price-save-badge">
                立省 ¥{formatPrice(product.originalPrice! - product.price)}
              </span>
            )}
          </div>

          <div className="product-seller-premium">
            {product.sellerAvatar ? (
              <>
                <div className="seller-avatar-premium">
                  <img src={product.sellerAvatar} alt={sellerName} width="32" height="32" />
                  <div className="seller-pulse-ring" />
                </div>
                <div className="seller-body-premium">
                  <span className="seller-name-premium">{sellerName}</span>
                  <span className="seller-note-premium">点击咨询</span>
                </div>
              </>
            ) : (
              <div className="seller-body-premium">
                <span className="seller-name-premium">{sellerName}</span>
                <span className="seller-note-premium">匿名用户</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  )
})

ProductCard.displayName = 'ProductCard'
