import { ChevronRight, Sparkles } from 'lucide-react';
import { Link } from 'react-router-dom';
import placeholderImage from '@/assets/placeholder.png';
import { Image } from '@/components/ui/Image';

interface SimilarProduct {
    id: string;
    title: string;
    price: number;
    images?: string[];
}

interface SimilarProductsProps {
    products: SimilarProduct[] | undefined;
}

export function SimilarProducts({ products }: SimilarProductsProps) {
    return (
        <div className="pdp-similar-section">
            <div className="pdp-section-header">
                <div className="pdp-section-accent" />
                <h3 className="pdp-section-title">
                    <Sparkles size={20} />
                    AI推荐相似商品
                </h3>
                <span className="pdp-section-badge">基于商品特征智能匹配</span>
            </div>

            {products && products.length > 0 ? (
                <>
                    <div className="pdp-similar-grid">
                        {products.slice(0, 4).map(item => (
                            <Link key={item.id} to={`/products/${item.id}`} className="pdp-similar-card">
                                <div className="pdp-similar-image">
                                    <Image
                                        src={item.images?.[0] || placeholderImage}
                                        alt={item.title}
                                        loading="lazy"
                                        placeholder="skeleton"
                                        style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                    />
                                </div>
                                <div className="pdp-similar-content">
                                    <h4 className="pdp-similar-title">{item.title}</h4>
                                    <div className="pdp-similar-price">¥{item.price.toFixed(0)}</div>
                                </div>
                            </Link>
                        ))}
                    </div>
                    <div className="pdp-similar-footer">
                        <Link to="/products" className="pdp-similar-more">
                            <span>查看更多相似商品</span>
                            <ChevronRight size={16} />
                        </Link>
                    </div>
                </>
            ) : (
                <div className="pdp-similar-empty">
                    <p>暂无相似商品推荐</p>
                </div>
            )}
        </div>
    );
}
