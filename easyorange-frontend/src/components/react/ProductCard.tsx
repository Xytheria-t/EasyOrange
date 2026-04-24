import React from 'react'
import { Heart, MessageCircle, Eye, MapPin, Clock } from 'lucide-react'
import { Product } from '../../types'
import { formatPrice, formatRelativeTime } from '../../utils'

const CONDITION_LABELS: Record<string, string> = {
  NEW: '全新',
  LIKE_NEW: '近乎全新',
  GOOD: '良好',
  FAIR: '一般',
  POOR: '较差',
}

const CONDITION_ICONS: Record<string, string> = {
  NEW: '✨',
  LIKE_NEW: '🌟',
  GOOD: '✓',
  FAIR: '○',
  POOR: '△',
}

interface ProductCardProps {
  product: Product
  onFavorite?: (id: number, isFavorited: boolean) => void
  isFavorited?: boolean
  onViewDetails?: (id: number) => void
  style?: React.CSSProperties
}

export const ProductCard: React.FC<ProductCardProps> = ({
  product,
  onFavorite,
  isFavorited = false,
  onViewDetails,
  style,
}) => {
  const imageUrl = product.images?.[0] || 'https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=800&auto=format&fit=crop'
  const conditionLabel = CONDITION_LABELS[product.condition] || product.condition
  const conditionIcon = CONDITION_ICONS[product.condition] || '○'
  const hasDiscount = product.originalPrice != null && product.originalPrice > product.price
  const discountPercent = hasDiscount
    ? Math.round((1 - product.price / product.originalPrice!) * 100)
    : 0
  const isHot = product.isHot || (product.views != null && product.views > 200)
  const quickLocation = product.location?.trim() || '校内面交'
  const sellerName = product.sellerName || '匿名用户'

  const handleFavoriteClick = (e: React.MouseEvent) => {
    e.stopPropagation()
    onFavorite?.(product.id, !isFavorited)
  }

  return (
    <article
      className="product-card"
      style={style}
      onClick={() => onViewDetails?.(product.id)}
      role="button"
      tabIndex={0}
      onKeyDown={(e) => e.key === 'Enter' && onViewDetails?.(product.id)}
    >
      {/* Image area */}
      <figure className="product-image">
        <img
          src={imageUrl}
          alt={product.title}
          loading="lazy"
          decoding="async"
        />

        {/* Badges */}
        <div className="product-badges">
          <span className="badge badge-condition">
            {conditionIcon} {conditionLabel}
          </span>
          {hasDiscount && (
            <span className="badge badge-discount">{discountPercent}折</span>
          )}
          {isHot && (
            <span className="badge badge-hot">热门</span>
          )}
        </div>

        {/* Quick meta pills */}
        <div className="product-quick-meta">
          <span className="product-quick-pill">
            <MapPin size={13} strokeWidth={2.5} /> {quickLocation}
          </span>
          {product.createTime && (
            <span className="product-quick-pill">
              <Clock size={13} strokeWidth={2.5} /> {formatRelativeTime(product.createTime)}
            </span>
          )}
        </div>

        {/* Action buttons */}
        <div className="product-actions">
          <button
            className={`action-icon favorite-btn ${isFavorited ? 'favorited' : ''}`}
            onClick={handleFavoriteClick}
            aria-label={isFavorited ? '取消收藏' : '收藏'}
            aria-pressed={isFavorited}
          >
            <Heart size={18} fill={isFavorited ? 'currentColor' : 'none'} strokeWidth={2} />
          </button>
          {product.sellerId && (
            <button
              className="action-icon contact-btn"
              onClick={(e) => { e.stopPropagation() }}
              aria-label="联系卖家"
            >
              <MessageCircle size={18} strokeWidth={2} />
            </button>
          )}
          <button
            className="action-icon view-btn"
            onClick={(e) => { e.stopPropagation(); onViewDetails?.(product.id) }}
            aria-label="查看详情"
          >
            <Eye size={18} strokeWidth={2} />
          </button>
        </div>
      </figure>

      {/* Info area */}
      <div className="product-info">
        {/* Eyebrow row */}
        <div className="product-eyebrow">
          {(product.category || product.categoryName) && (
            <span className="product-category">
              {product.category || product.categoryName}
            </span>
          )}
          <span className={`product-signal ${isHot ? 'is-hot' : ''}`}>
            {product.favorites != null && product.favorites > 0
              ? `${product.favorites}人收藏`
              : isHot
              ? '热度精选'
              : '校园在售'}
          </span>
        </div>

        {/* Title */}
        <h3 className="product-title">{product.title}</h3>

        {/* Stat chips */}
        <div className="product-stat-row">
          <span className="product-info-chip">
            <Eye size={13} strokeWidth={2.5} /> {product.views || 0} 浏览
          </span>
          {product.favorites != null && product.favorites > 0 && (
            <span className="product-info-chip">
              <Heart size={13} strokeWidth={2.5} /> {product.favorites} 收藏
            </span>
          )}
        </div>

        {/* Footer: price + seller */}
        <div className="product-footer">
          <div className="product-price">
            <div className="product-price-row">
              <span className="price-current">¥{formatPrice(product.price)}</span>
              {hasDiscount && (
                <span className="price-original">¥{formatPrice(product.originalPrice!)}</span>
              )}
            </div>
            {hasDiscount && (
              <span className="price-note">
                立省 ¥{formatPrice(product.originalPrice! - product.price)}
              </span>
            )}
          </div>

          <div className="product-seller">
            {product.sellerAvatar ? (
              <>
                <div className="seller-avatar">
                  <img src={product.sellerAvatar} alt={sellerName} />
                </div>
                <div className="seller-body">
                  <span className="seller-name">{sellerName}</span>
                  <span className="seller-note">点击咨询</span>
                </div>
              </>
            ) : (
              <div className="seller-body">
                <span className="seller-name">{sellerName}</span>
                <span className="seller-note">匿名用户</span>
              </div>
            )}
          </div>
        </div>
      </div>
    </article>
  )
}
