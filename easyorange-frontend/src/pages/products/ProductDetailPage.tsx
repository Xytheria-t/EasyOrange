import { zodResolver } from '@hookform/resolvers/zod';
import { useMutation, useQueryClient } from '@tanstack/react-query';
import './product-detail.css';
import {
    ArrowLeft,
    ChevronRight as BreadcrumbSep,
    Check,
    ChevronRight,
    Clock,
    Copy,
    Eye,
    Info,
    MapPin,
    MessageCircle,
    Pencil,
    Send,
    Shield,
    ShoppingCart,
    Sparkles,
    Star,
    Tag,
    TrendingUp,
    User,
    Zap,
} from 'lucide-react';
import { useEffect, useState } from 'react';
import { useForm } from 'react-hook-form';
import { Link, useNavigate, useParams } from 'react-router-dom';
import { messageApi } from '@/api/messageApi';
import { productApi } from '@/api/productApi';
import placeholderImage from '@/assets/placeholder.png';
import {
    Dialog,
    DialogContent,
    DialogFooter,
    DialogHeader,
    DialogTitle,
    Input,
    Label,
    Textarea,
} from '@/components/ui';
import { Button } from '@/components/ui/button';
import { Image, preloadImages } from '@/components/ui/Image';
import { CONDITION_LABEL_MAP, STATUS_LABEL_MAP } from '@/constants';
import { useCreateOrder, useProduct, useSimilarProducts } from '@/hooks';
import { type OrderFormData, orderFormSchema } from '@/schemas/productDetailSchema';
import { useAuthStore } from '@/store/authStore';
import { useUIStore } from '@/store/uiStore';
import type { ChatMessage } from '@/types/message';
import { formatRelativeTime } from '@/utils';
import { normalizeChatMessages } from '@/utils/message';
import { ProductGallery } from './components/ProductGallery';

function ProductDetailPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { data: product, isLoading } = useProduct(id ?? '');
    const { data: similarProducts } = useSimilarProducts(id ?? '');
    const { token, user } = useAuthStore();
    const addToast = useUIStore(s => s.addToast);

    useEffect(() => {
        if (similarProducts && similarProducts.length > 0) {
            const similarImages = similarProducts
                .slice(0, 4)
                .map(item => item.images?.[0])
                .filter(Boolean) as string[];
            if (similarImages.length > 0) {
                preloadImages(similarImages, { width: 300, format: 'webp', quality: 75 }).catch(() => {});
            }
        }
    }, [similarProducts]);

    const queryClient = useQueryClient();
    const createOrder = useCreateOrder();
    const [showOrderModal, setShowOrderModal] = useState(false);
    const [showShareModal, setShowShareModal] = useState(false);
    const [showChatModal, setShowChatModal] = useState(false);
    const orderForm = useForm<OrderFormData>({
        resolver: zodResolver(orderFormSchema),
        defaultValues: { phone: '', remark: '' },
        reValidateMode: 'onChange',
    });
    const [chatMessage, setChatMessage] = useState('');
    const [chatMessages, setChatMessages] = useState<ChatMessage[]>([]);
    const [copied, setCopied] = useState(false);

    const productId = id ?? '';

    useEffect(() => {
        if (productId) {
            productApi.incrementView(productId).catch(() => {});
        }
    }, [productId]);

    useEffect(() => {
        if (!showChatModal || !product?.sellerId || !token) {
            return;
        }
        const loadChatHistory = async () => {
            try {
                const response = await messageApi.getConversation(product.sellerId);
                setChatMessages(normalizeChatMessages(response.data ?? []).reverse());
            } catch {
                setChatMessages([]);
            }
        };
        loadChatHistory();
    }, [showChatModal, product?.sellerId, token]);

    const resubmitForReview = useMutation({
        mutationFn: async () => {
            await productApi.submitForReview(productId);
        },
        onSuccess: () => {
            addToast({ type: 'success', message: '已重新提交审核' });
            queryClient.invalidateQueries({ queryKey: ['product', productId] });
        },
        onError: () => {
            addToast({ type: 'error', message: '重新提交失败，请重试' });
        },
    });

    if (isLoading) {
        return (
            <div className="pdp-loading">
                <div className="pdp-loading-ambient">
                    <div className="pdp-ambient-orb pdp-ambient-orb-1" />
                    <div className="pdp-ambient-orb pdp-ambient-orb-2" />
                </div>
                <div className="pdp-loading-content">
                    <div className="pdp-loading-ring" />
                    <span className="pdp-loading-text">加载商品详情...</span>
                </div>
            </div>
        );
    }

    if (!product) {
        return (
            <div className="pdp-empty">
                <div className="pdp-empty-visual">
                    <div className="pdp-empty-icon">📦</div>
                    <div className="pdp-empty-ring" />
                </div>
                <h3 className="pdp-empty-title">商品不存在</h3>
                <p className="pdp-empty-desc">该商品可能已下架或被删除</p>
                <Button className="pdp-empty-btn" onClick={() => navigate('/products')}>
                    <ArrowLeft size={16} />
                    返回商城
                </Button>
            </div>
        );
    }

    const images = product.images?.length > 0 ? product.images : [placeholderImage];
    const isOwner = user && product.sellerId === user.userId;
    const conditionLabel = product.conditionLevel
        ? (CONDITION_LABEL_MAP[product.conditionLevel] ?? product.condition)
        : product.condition;
    const statusLabel = STATUS_LABEL_MAP[product.status as keyof typeof STATUS_LABEL_MAP] ?? product.status;
    const isOnline = product.status === 'ONLINE';
    const isSold = product.status === 'SOLD';
    const isPendingReview = product.status === 'PENDING_REVIEW';
    const isRejected = product.status === 'REJECTED';
    const hasDiscount = product.originalPrice != null && product.originalPrice > product.price;
    const discountPercent = hasDiscount ? Math.round((1 - product.price / (product.originalPrice as number)) * 100) : 0;

    const handleBuyClick = () => {
        if (!token) {
            navigate(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
            return;
        }
        if (!product || isOwner) {
            return;
        }
        setShowOrderModal(true);
    };

    const handleContactSeller = async () => {
        if (!token) {
            navigate(`/login?redirect=${encodeURIComponent(window.location.pathname)}`);
            return;
        }
        if (!product || isOwner) {
            return;
        }

        // Prefetch chat data and await completion before navigating.
        // This ensures query.state.data exists when ChatWindowPage mounts,
        // preventing the TanStack Query + React 19 + StrictMode infinite loop:
        // render-time getOptimisticResult uses mounted=FALSE → optimistic fetchStatus,
        // subscribe+updateResult switches to mounted=TRUE → different snapshot →
        // useSyncExternalStore detects mismatch → forceStoreRerender → nest to 50.
        const targetUserId = product.sellerId;
        try {
            await queryClient.prefetchQuery({
                queryKey: ['chat', 'messages', targetUserId],
                queryFn: () =>
                    messageApi.getConversation(targetUserId).then(res => {
                        const data = (res.data ?? []) as unknown[];
                        return data.slice(-50);
                    }),
                staleTime: Infinity,
            });
        } catch {
            // Navigate even if prefetch fails — ChatWindowPage handles error state natively
        }

        navigate(`/messages/${targetUserId}`);
    };

    const handleShare = () => {
        setShowShareModal(true);
    };

    const handleCopyLink = async () => {
        try {
            await navigator.clipboard.writeText(window.location.href);
            setCopied(true);
            addToast({ type: 'success', message: '链接已复制到剪贴板' });
            setTimeout(() => setCopied(false), 2000);
        } catch {
            addToast({ type: 'error', message: '复制失败，请手动复制' });
        }
    };

    const handleSendMessage = async () => {
        if (!chatMessage.trim() || !product?.sellerId) {
            return;
        }
        try {
            await messageApi.sendMessage({
                receiverId: product.sellerId,
                content: chatMessage.trim(),
            });
            setChatMessages(prev => [
                ...prev,
                {
                    id: String(Date.now()),
                    senderId: user?.userId ?? '',
                    receiverId: product.sellerId,
                    content: chatMessage.trim(),
                    type: 'TEXT',
                    status: 'SENT',
                    createTime: new Date().toISOString(),
                    readTime: null,
                    recalledAt: null,
                },
            ]);
            setChatMessage('');
            addToast({ type: 'success', message: '消息已发送' });
        } catch {
            addToast({ type: 'error', message: '发送失败，请重试' });
        }
    };

    const handleQuickReply = (text: string) => {
        setChatMessage(text);
    };

    const handleSubmitOrder = orderForm.handleSubmit(async values => {
        try {
            const orderId = await createOrder.mutateAsync({
                items: [{ productId: product.id, quantity: 1 }],
                phone: values.phone.trim(),
                remark: values.remark.trim() || undefined,
            });
            setShowOrderModal(false);
            orderForm.reset();
            addToast({ type: 'success', message: '订单创建成功' });
            navigate(`/orders/${orderId}`);
        } catch {
            addToast({ type: 'error', message: '创建订单失败，请重试' });
        }
    });

    const quickReplies = ['这个商品还在吗？', '能便宜点吗？', '可以面交吗？', '商品有什么瑕疵吗？'];

    return (
        <div className="pdp-page">
            <div className="pdp-ambient">
                <div className="pdp-ambient-orb pdp-ambient-orb-1" />
                <div className="pdp-ambient-orb pdp-ambient-orb-2" />
                <div className="pdp-ambient-orb pdp-ambient-orb-3" />
                <div className="pdp-ambient-noise" />
            </div>

            <div className="pdp-container">
                <nav className="pdp-breadcrumb">
                    <Button variant="ghost" className="pdp-back-btn" onClick={() => navigate(-1)}>
                        <ArrowLeft size={16} />
                        返回
                    </Button>
                    <div className="pdp-breadcrumb-trail">
                        <Button
                            type="button"
                            variant="link"
                            className="pdp-breadcrumb-item"
                            onClick={() => navigate('/products')}
                        >
                            商城
                        </Button>
                        <BreadcrumbSep size={14} className="pdp-breadcrumb-sep" />
                        {product.categoryName && (
                            <>
                                <span className="pdp-breadcrumb-item">{product.categoryName}</span>
                                <BreadcrumbSep size={14} className="pdp-breadcrumb-sep" />
                            </>
                        )}
                        <span className="pdp-breadcrumb-current">{product.title}</span>
                    </div>
                </nav>

                <div className="pdp-hero">
                    <ProductGallery images={images} isSold={isSold} onShare={handleShare} />

                    <div className="pdp-info">
                        <div className="pdp-info-sticky">
                            <div className="pdp-title-section">
                                <div className="pdp-title-badges">
                                    {isPendingReview ? (
                                        <span
                                            className="pdp-status-chip"
                                            style={{
                                                background: '#FEF3C7',
                                                color: '#D97706',
                                                border: '1px solid #FDE68A',
                                            }}
                                        >
                                            {statusLabel}
                                        </span>
                                    ) : isRejected ? (
                                        <span
                                            className="pdp-status-chip"
                                            style={{
                                                background: '#FEE2E2',
                                                color: '#DC2626',
                                                border: '1px solid #FECACA',
                                            }}
                                        >
                                            {statusLabel}
                                        </span>
                                    ) : (
                                        <span
                                            className={`pdp-status-chip ${isOnline ? 'online' : isSold ? 'sold' : 'offline'}`}
                                        >
                                            {statusLabel}
                                        </span>
                                    )}
                                    {conditionLabel && <span className="pdp-condition-chip">{conditionLabel}</span>}
                                    {product.categoryName && (
                                        <span className="pdp-category-chip">
                                            <Tag size={12} />
                                            {product.categoryName}
                                        </span>
                                    )}
                                </div>
                                <h1 className="pdp-title">{product.title}</h1>
                                <div className="pdp-meta-row">
                                    <span className="pdp-meta-item">
                                        <Eye size={14} />
                                        {product.views} 次浏览
                                    </span>
                                    <span className="pdp-meta-item">
                                        <Clock size={14} />
                                        {formatRelativeTime(product.createTime)}发布
                                    </span>
                                </div>
                            </div>

                            <div className="pdp-price-card">
                                <div className="pdp-price-main">
                                    <span className="pdp-price-value">¥{product.price.toFixed(2)}</span>
                                    {hasDiscount && (
                                        <div className="pdp-price-discount">
                                            <span className="pdp-price-original">
                                                ¥{(product.originalPrice as number).toFixed(2)}
                                            </span>
                                            <span className="pdp-discount-badge">-{discountPercent}%</span>
                                        </div>
                                    )}
                                </div>
                                {hasDiscount && (
                                    <div className="pdp-savings">
                                        比原价省{' '}
                                        <strong>
                                            ¥{((product.originalPrice as number) - product.price).toFixed(2)}
                                        </strong>
                                    </div>
                                )}
                            </div>

                            <div className="pdp-ai-pricing-card">
                                <div className="pdp-ai-pricing-header">
                                    <div className="pdp-ai-badge">
                                        <Sparkles size={14} />
                                        <span>AI智能估价</span>
                                    </div>
                                    <span className="pdp-ai-confidence">置信度 95%</span>
                                </div>
                                <div className="pdp-ai-pricing-body">
                                    <div className="pdp-ai-price-range">
                                        <div className="pdp-ai-price-item">
                                            <span className="pdp-ai-price-label">市场均价</span>
                                            <span className="pdp-ai-price-value">
                                                ¥{((product.price || 100) * 1.15).toFixed(0)}
                                            </span>
                                        </div>
                                        <div className="pdp-ai-price-divider" />
                                        <div className="pdp-ai-price-item">
                                            <span className="pdp-ai-price-label">低价区间</span>
                                            <span className="pdp-ai-price-value low">
                                                ¥{((product.price || 100) * 0.85).toFixed(0)}
                                            </span>
                                        </div>
                                        <div className="pdp-ai-price-divider" />
                                        <div className="pdp-ai-price-item highlight">
                                            <span className="pdp-ai-price-label">当前定价</span>
                                            <span className="pdp-ai-price-value">¥{product.price.toFixed(0)}</span>
                                        </div>
                                    </div>
                                    <div className="pdp-ai-pricing-analysis">
                                        <div className="pdp-ai-analysis-icon">
                                            <TrendingUp size={14} />
                                        </div>
                                        <p className="pdp-ai-analysis-text">
                                            该商品定价<span className="highlight">合理偏低</span>
                                            ，相比同类商品具有价格优势，性价比突出
                                        </p>
                                    </div>
                                    <div className="pdp-ai-pricing-tags">
                                        <span className="pdp-ai-tag">
                                            <Zap size={10} />
                                            价格优势
                                        </span>
                                        <span className="pdp-ai-tag">
                                            <Star size={10} />
                                            值得购买
                                        </span>
                                        <span className="pdp-ai-tag">
                                            <TrendingUp size={10} />
                                            热门品类
                                        </span>
                                    </div>
                                </div>
                            </div>

                            <div className="pdp-details-card">
                                <div className="pdp-detail-item">
                                    <div className="pdp-detail-icon-wrap">
                                        <MapPin size={16} />
                                    </div>
                                    <div className="pdp-detail-content">
                                        <span className="pdp-detail-label">交易地点</span>
                                        <span className="pdp-detail-value">{product.location || '未指定'}</span>
                                    </div>
                                </div>

                                <div className="pdp-detail-item">
                                    <div className="pdp-detail-icon-wrap">
                                        <Shield size={16} />
                                    </div>
                                    <div className="pdp-detail-content">
                                        <span className="pdp-detail-label">商品成色</span>
                                        <span className="pdp-detail-value">{conditionLabel || '未标注'}</span>
                                    </div>
                                </div>

                                <div className="pdp-detail-item pdp-seller-item">
                                    <div className="pdp-detail-icon-wrap pdp-seller-avatar-wrap">
                                        <User size={16} />
                                    </div>
                                    <div className="pdp-detail-content">
                                        <span className="pdp-detail-label">资产方</span>
                                        <span className="pdp-detail-value">{product.sellerName}</span>
                                    </div>
                                    {!isOwner && (
                                        <Button className="pdp-contact-btn" onClick={handleContactSeller}>
                                            <MessageCircle size={14} />
                                            联系
                                        </Button>
                                    )}
                                </div>
                            </div>

                            <div className="pdp-actions">
                                {isOwner ? (
                                    <div style={{ display: 'flex', gap: '0.75rem', flexWrap: 'wrap' }}>
                                        <Button
                                            className="pdp-btn pdp-btn-primary"
                                            onClick={() => navigate(`/products/${id}/edit`)}
                                            disabled={isPendingReview}
                                        >
                                            <Pencil size={18} />
                                            编辑商品
                                        </Button>
                                        {isRejected && (
                                            <Button
                                                className="pdp-btn pdp-btn-primary"
                                                onClick={() => resubmitForReview.mutate()}
                                                disabled={resubmitForReview.isPending}
                                            >
                                                {resubmitForReview.isPending ? '提交中...' : '修改并重新提交'}
                                            </Button>
                                        )}
                                    </div>
                                ) : (
                                    <>
                                        <Button
                                            className="pdp-btn pdp-btn-primary"
                                            onClick={handleBuyClick}
                                            disabled={isSold}
                                        >
                                            <ShoppingCart size={18} />
                                            {isSold ? '已售出' : '立即购买'}
                                        </Button>
                                        <Button
                                            variant="outline"
                                            className="pdp-btn pdp-btn-secondary"
                                            onClick={handleContactSeller}
                                        >
                                            <MessageCircle size={18} />
                                            联系资产方
                                        </Button>
                                    </>
                                )}
                            </div>
                        </div>
                    </div>
                </div>

                <div className="pdp-description-section">
                    <div className="pdp-section-header">
                        <div className="pdp-section-accent" />
                        <h3 className="pdp-section-title">商品描述</h3>
                    </div>
                    <div className="pdp-description-body">
                        <p className="pdp-description-text">
                            {product.description || '资产方暂未填写详细描述，可通过下方「联系资产方」了解更多信息'}
                        </p>
                    </div>
                </div>

                <div className="pdp-section-divider" />

                <div className="pdp-similar-section">
                    <div className="pdp-section-header">
                        <div className="pdp-section-accent" />
                        <h3 className="pdp-section-title">
                            <Sparkles size={20} />
                            AI推荐相似商品
                        </h3>
                        <span className="pdp-section-badge">基于商品特征智能匹配</span>
                    </div>

                    {similarProducts && similarProducts.length > 0 ? (
                        <>
                            <div className="pdp-similar-grid">
                                {similarProducts.slice(0, 4).map(item => (
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
                                <Button
                                    variant="ghost"
                                    className="pdp-similar-more"
                                    onClick={() => navigate('/products')}
                                >
                                    <span>查看更多相似商品</span>
                                    <ChevronRight size={16} />
                                </Button>
                            </div>
                        </>
                    ) : (
                        <div className="pdp-similar-empty">
                            <p>暂无相似商品推荐</p>
                        </div>
                    )}
                </div>

                <div className="pdp-section-divider" />

                <div className="pdp-ai-tips-section">
                    <div className="pdp-ai-tips-card">
                        <div className="pdp-ai-tips-icon">
                            <Info size={20} />
                        </div>
                        <div className="pdp-ai-tips-content">
                            <h4 className="pdp-ai-tips-title">AI助手温馨提示</h4>
                            <ul className="pdp-ai-tips-list">
                                <li>建议与资产方确认资产细节后再进行交易</li>
                                <li>优先选择校内面交，安全便捷</li>
                                <li>如遇纠纷可联系平台客服协助处理</li>
                            </ul>
                        </div>
                    </div>
                </div>
            </div>

            <Dialog open={showOrderModal} onOpenChange={open => !open && setShowOrderModal(false)}>
                <DialogContent className="sm:max-w-[520px]">
                    <DialogHeader>
                        <DialogTitle>确认购买</DialogTitle>
                    </DialogHeader>

                    <div className="pdp-modal-product">
                        <div className="pdp-modal-product-image">
                            {images.length > 0 ? (
                                <Image
                                    src={images[0]}
                                    alt={product.title}
                                    loading="lazy"
                                    placeholder="skeleton"
                                    style={{ width: '100%', height: '100%', objectFit: 'cover' }}
                                />
                            ) : (
                                <div className="pdp-modal-product-placeholder">
                                    <ShoppingCart size={20} />
                                </div>
                            )}
                        </div>
                        <div className="pdp-modal-product-info">
                            <p className="pdp-modal-product-name">{product.title}</p>
                            <p className="pdp-modal-product-price">¥{product.price.toFixed(2)}</p>
                        </div>
                    </div>

                    <div className="pdp-modal-form">
                        <div className="pdp-form-group">
                            <Label htmlFor="order-location">交易地点</Label>
                            <Button
                                type="button"
                                variant="outline"
                                className="w-full justify-between"
                                onClick={() => {
                                    setShowOrderModal(false);
                                    navigate(`/messages/${product.sellerId}`);
                                }}
                            >
                                <span className="text-muted-foreground">私聊资产方协商交易地点</span>
                                <MessageCircle size={16} className="text-muted-foreground" />
                            </Button>
                        </div>
                        <div className="pdp-form-group">
                            <Label htmlFor="order-phone">
                                联系电话 <span className="pdp-form-required">*</span>
                            </Label>
                            <Input
                                id="order-phone"
                                type="tel"
                                maxLength={11}
                                placeholder="请输入手机号"
                                {...orderForm.register('phone')}
                            />
                            {orderForm.formState.errors.phone && (
                                <p className="pdp-form-error">{orderForm.formState.errors.phone.message}</p>
                            )}
                        </div>
                        <div className="pdp-form-group">
                            <Label htmlFor="order-remark">备注</Label>
                            <Textarea
                                id="order-remark"
                                placeholder="选填，对资产方留言"
                                rows={3}
                                {...orderForm.register('remark')}
                            />
                        </div>
                    </div>

                    <DialogFooter className="flex-row gap-2.5 sm:justify-end">
                        <Button variant="outline" onClick={() => setShowOrderModal(false)}>
                            取消
                        </Button>
                        <Button onClick={handleSubmitOrder} disabled={createOrder.isPending}>
                            {createOrder.isPending ? '提交中...' : '提交订单'}
                        </Button>
                    </DialogFooter>
                </DialogContent>
            </Dialog>

            <Dialog open={showShareModal} onOpenChange={open => !open && setShowShareModal(false)}>
                <DialogContent className="sm:max-w-[520px]">
                    <DialogHeader>
                        <DialogTitle>分享商品</DialogTitle>
                    </DialogHeader>

                    <div className="pdp-share-content">
                        <div className="pdp-share-platforms">
                            <Button variant="ghost" className="pdp-share-platform pdp-share-wechat">
                                <div className="pdp-share-icon">
                                    <MessageCircle size={24} />
                                </div>
                                <span>微信</span>
                            </Button>
                            <Button variant="ghost" className="pdp-share-platform pdp-share-qq">
                                <div className="pdp-share-icon">Q</div>
                                <span>QQ</span>
                            </Button>
                            <Button variant="ghost" className="pdp-share-platform pdp-share-weibo">
                                <div className="pdp-share-icon">微</div>
                                <span>微博</span>
                            </Button>
                            <Button
                                variant="ghost"
                                className="pdp-share-platform pdp-share-copy"
                                onClick={handleCopyLink}
                            >
                                <div className="pdp-share-icon">
                                    {copied ? <Check size={24} /> : <Copy size={24} />}
                                </div>
                                <span>{copied ? '已复制' : '复制链接'}</span>
                            </Button>
                        </div>

                        <div className="pdp-share-link">
                            <Label htmlFor="share-link">商品链接</Label>
                            <div className="relative flex items-center">
                                <Input
                                    id="share-link"
                                    type="text"
                                    value={window.location.href}
                                    readOnly
                                    className="pr-10"
                                />
                                <Button
                                    variant="ghost"
                                    size="icon"
                                    className="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8"
                                    onClick={handleCopyLink}
                                >
                                    {copied ? <Check size={16} /> : <Copy size={16} />}
                                </Button>
                            </div>
                        </div>
                    </div>
                </DialogContent>
            </Dialog>

            <Dialog open={showChatModal} onOpenChange={open => !open && setShowChatModal(false)}>
                <DialogContent className="sm:max-w-[520px]">
                    <DialogHeader>
                        <DialogTitle>
                            <MessageCircle size={18} />
                            联系资产方
                        </DialogTitle>
                    </DialogHeader>

                    <div className="pdp-chat-seller">
                        <div className="pdp-chat-seller-avatar">
                            <User size={20} />
                        </div>
                        <div className="pdp-chat-seller-info">
                            <span className="pdp-chat-seller-name">{product.sellerName}</span>
                            <span className="pdp-chat-product">{product.title}</span>
                        </div>
                    </div>

                    <div className="pdp-chat-messages">
                        {chatMessages.length > 0 ? (
                            chatMessages.map(msg => (
                                <div
                                    key={msg.id}
                                    className={`pdp-chat-message ${msg.senderId === user?.userId ? 'mine' : 'theirs'}`}
                                >
                                    <div className="pdp-chat-bubble">{msg.content}</div>
                                    <span className="pdp-chat-time">{formatRelativeTime(msg.createTime)}</span>
                                </div>
                            ))
                        ) : (
                            <div className="pdp-chat-empty">
                                <MessageCircle size={32} />
                                <p>开始与资产方聊天吧</p>
                            </div>
                        )}
                    </div>

                    <div className="pdp-chat-quick-replies">
                        {quickReplies.map(text => (
                            <Button
                                key={text}
                                variant="outline"
                                size="sm"
                                className="pdp-chat-quick-reply"
                                onClick={() => handleQuickReply(text)}
                            >
                                {text}
                            </Button>
                        ))}
                    </div>

                    <div className="relative flex items-center gap-2">
                        <Input
                            type="text"
                            value={chatMessage}
                            onChange={e => setChatMessage(e.target.value)}
                            placeholder="输入消息..."
                            onKeyDown={e => e.key === 'Enter' && handleSendMessage()}
                            className="flex-1"
                        />
                        <Button size="icon" onClick={handleSendMessage} disabled={!chatMessage.trim()}>
                            <Send size={18} />
                        </Button>
                    </div>
                </DialogContent>
            </Dialog>
        </div>
    );
}

export default ProductDetailPage;
