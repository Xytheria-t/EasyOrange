import { MessageSquare, Star, ThumbsUp, User } from 'lucide-react';
import { Button } from '@/components/ui/button';
import { formatRelativeTime } from '@/utils';

interface Review {
    id: number;
    username?: string;
    rating: number;
    content: string;
    createTime: string;
    likes?: number;
}

interface ProductRatingsProps {
    reviews: Review[];
    reviewTotal: number;
    avgRating: string;
    isLoading: boolean;
    canReview: boolean;
    onReviewClick: () => void;
}

export function ProductRatings({
    reviews,
    reviewTotal,
    avgRating,
    isLoading,
    canReview,
    onReviewClick,
}: ProductRatingsProps) {
    return (
        <div className="pdp-reviews-section">
            <div className="pdp-section-header">
                <div className="pdp-section-accent" />
                <h3 className="pdp-section-title">
                    <MessageSquare size={20} />
                    商品评价
                </h3>
                <div className="pdp-reviews-stats">
                    <span className="pdp-reviews-avg">
                        <Star size={14} fill="currentColor" />
                        {avgRating}
                    </span>
                    <span className="pdp-reviews-count">{reviewTotal} 条评价</span>
                </div>
            </div>

            {isLoading ? (
                <div className="pdp-reviews-loading">
                    <div className="pdp-loading-ring" />
                    <span>加载评价中...</span>
                </div>
            ) : reviews.length > 0 ? (
                <div className="pdp-reviews-list">
                    {reviews.slice(0, 5).map(review => (
                        <div key={review.id} className="pdp-review-item">
                            <div className="pdp-review-header">
                                <div className="pdp-review-user">
                                    <div className="pdp-review-avatar">
                                        <User size={16} />
                                    </div>
                                    <span className="pdp-review-username">{review.username || '匿名用户'}</span>
                                </div>
                                <div className="pdp-review-meta">
                                    <div className="pdp-review-rating">
                                        {[1, 2, 3, 4, 5].map(star => (
                                            <Star
                                                key={star}
                                                size={14}
                                                fill={star <= (review.rating || 5) ? 'currentColor' : 'none'}
                                            />
                                        ))}
                                    </div>
                                    <span className="pdp-review-time">
                                        {formatRelativeTime(review.createTime || new Date().toISOString())}
                                    </span>
                                </div>
                            </div>
                            <p className="pdp-review-content">{review.content || '用户未填写评价内容'}</p>
                            {(review.likes ?? 0) > 0 && (
                                <div className="pdp-review-footer">
                                    <Button variant="ghost" size="sm" className="pdp-review-like">
                                        <ThumbsUp size={14} />
                                        <span>{String(review.likes)}</span>
                                    </Button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            ) : (
                <div className="pdp-reviews-empty">
                    <MessageSquare size={36} />
                    <p>暂无评价</p>
                    <span>成为第一个评价的人吧</span>
                </div>
            )}

            {canReview && (
                <div className="pdp-reviews-action">
                    <Button variant="outline" className="pdp-btn pdp-btn-secondary" onClick={onReviewClick}>
                        <Star size={16} />
                        发表评价
                    </Button>
                </div>
            )}
        </div>
    );
}
