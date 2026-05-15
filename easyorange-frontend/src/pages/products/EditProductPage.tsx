import { useState, useRef, useMemo } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Camera, X, Loader2, ArrowLeft, Package, Tag, MapPin, FileText, Settings, Trash2, AlertTriangle, Sparkles, Brain } from 'lucide-react';
import { useProduct, useUpdateProduct, useDeleteProduct, useCategories } from '@/hooks';
import { uploadFile } from '@/api/uploadApi';
import { compressImage } from '@/utils/imageCompress';
import { CONDITION_LABEL_MAP } from '@/constants';
import './edit-product.css';

interface FormState {
    name: string;
    description: string;
    price: string;
    originalPrice: string;
    categoryId: string;
    conditionLevel: string;
    stock: string;
    location: string;
    contactMethod: string;
    imageUrls: string[];
}

interface FormErrors {
    name?: string;
    price?: string;
    categoryId?: string;
    conditionLevel?: string;
}

function EditProductPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { data: product, isLoading: isLoadingProduct } = useProduct(id ?? '');
    const updateProduct = useUpdateProduct(id ?? '');
    const deleteProduct = useDeleteProduct();
    const { data: categories } = useCategories();
    const [errors, setErrors] = useState<FormErrors>({});
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const initialForm = useMemo((): FormState => {
        if (!product) {
            return {
                name: '',
                description: '',
                price: '',
                originalPrice: '',
                categoryId: '',
                conditionLevel: '',
                stock: '1',
                location: '',
                contactMethod: '',
                imageUrls: [],
            };
        }
        return {
            name: product.title || '',
            description: product.description || '',
            price: product.price?.toString() || '',
            originalPrice: product.originalPrice?.toString() || '',
            categoryId: product.categoryId?.toString() || '',
            conditionLevel: product.conditionLevel?.toString() || '',
            stock: product.stock?.toString() || '1',
            location: product.location || '',
            contactMethod: product.contactMethod || '',
            imageUrls: product.images || [],
        };
    }, [product]);
    const [form, setForm] = useState<FormState>(initialForm);

    const validate = (): boolean => {
        const newErrors: FormErrors = {};
        if (!form.name.trim()) {newErrors.name = '请输入商品名称';}
        if (!form.price || Number(form.price) <= 0 || isNaN(Number(form.price))) {newErrors.price = '请输入有效价格';}
        if (!form.categoryId || Number(form.categoryId) <= 0) {newErrors.categoryId = '请选择商品类别';}
        if (!form.conditionLevel || Number(form.conditionLevel) <= 0) {newErrors.conditionLevel = '请选择新旧程度';}
        setErrors(newErrors);
        return Object.keys(newErrors).length === 0;
    };

    const updateField = <K extends keyof FormState>(field: K, value: FormState[K]) => {
        setForm(prev => ({ ...prev, [field]: value }));
        if (errors[field as keyof FormErrors]) {
            setErrors(prev => {
                const next = { ...prev };
                delete next[field as keyof FormErrors];
                return next;
            });
        }
    };

    const handleImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) {return;}

        for (const file of Array.from(files)) {
            if (form.imageUrls.length >= 9) {break;}
            if (!file.type.startsWith('image/')) {continue;}
            if (file.size > 10 * 1024 * 1024) {continue;}

            const index = form.imageUrls.length;
            setUploadingIndex(index);
            try {
                const compressed = await compressImage(file);
                const result = await uploadFile(compressed);
                if (result.data?.url) {
                    updateField('imageUrls', [...form.imageUrls, result.data.url]);
                }
            } catch (error) {
                console.error('图片上传失败:', error);
            } finally {
                setUploadingIndex(null);
            }
        }

        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleImageUrlRemove = (index: number) => {
        updateField('imageUrls', form.imageUrls.filter((_, i) => i !== index));
    };

    const handleSubmit = async () => {
        if (!validate()) {return;}
        try {
            await updateProduct.mutateAsync({
                name: form.name.trim(),
                description: form.description.trim(),
                price: Number(form.price),
                originalPrice: form.originalPrice ? Number(form.originalPrice) : undefined,
                categoryId: Number(form.categoryId),
                conditionLevel: Number(form.conditionLevel),
                stock: Number(form.stock) || 1,
                location: form.location.trim() || undefined,
                contactMethod: form.contactMethod.trim() || undefined,
                imageUrls: form.imageUrls,
            });
            navigate(`/products/${id}`);
        } catch {
            console.error('更新商品失败');
        }
    };

    const handleDelete = async () => {
        try {
            await deleteProduct.mutateAsync(id ?? '');
            navigate('/products');
        } catch {
            console.error('删除商品失败');
        }
    };

    if (isLoadingProduct) {
        return (
            <div className="edit-product-loading">
                <div className="edit-loading-spinner">
                    <Loader2 size={32} />
                </div>
                <span className="edit-loading-text">加载商品信息...</span>
            </div>
        );
    }

    if (!product) {
        return (
            <div className="edit-product-empty">
                <div className="edit-empty-icon">
                    <Package size={48} />
                </div>
                <p className="edit-empty-text">商品不存在</p>
                <button className="edit-empty-btn" onClick={() => navigate('/products')}>
                    返回商品列表
                </button>
            </div>
        );
    }

    const isSubmitting = updateProduct.isPending;

    return (
        <div className="edit-product-page">
            <div className="edit-product-bg">
                <div className="edit-orb edit-orb-1"></div>
                <div className="edit-orb edit-orb-2"></div>
            </div>

            <div className="edit-product-nav">
                <button onClick={() => navigate(-1)} className="edit-back-btn">
                    <ArrowLeft size={20} />
                </button>
                <h1 className="edit-nav-title">编辑商品</h1>
                <button 
                    className="edit-delete-btn-nav"
                    onClick={() => setShowDeleteConfirm(true)}
                >
                    <Trash2 size={18} />
                </button>
            </div>

            <div className="edit-product-content">
                <div className="edit-header-section">
                    <div className="edit-header-icon">
                        <Package size={24} />
                    </div>
                    <div className="edit-header-text">
                        <h2>修改商品信息</h2>
                        <p>更新商品详情后点击保存</p>
                    </div>
                </div>

                <div className="edit-ai-tip">
                    <div className="edit-ai-tip-icon">
                        <Brain size={16} />
                    </div>
                    <span>AI智能助手：完善商品信息可获得更多曝光</span>
                    <Sparkles size={14} className="edit-ai-sparkle" />
                </div>

                <div className="edit-form-card">
                    <div className="edit-form-section">
                        <div className="edit-section-header">
                            <div className="edit-section-icon">
                                <Camera size={18} />
                            </div>
                            <div className="edit-section-title">
                                <h3>商品图片</h3>
                                <span>最多上传9张，第一张为封面</span>
                            </div>
                        </div>
                        <div className="edit-image-grid">
                            {form.imageUrls.map((url, index) => (
                                <div key={index} className={`edit-image-item ${index === 0 ? 'is-cover' : ''}`}>
                                    <img src={url} alt={`商品图片 ${index + 1}`} />
                                    {index === 0 && <span className="edit-cover-badge">封面</span>}
                                    <button
                                        type="button"
                                        onClick={() => handleImageUrlRemove(index)}
                                        className="edit-image-remove"
                                    >
                                        <X size={14} />
                                    </button>
                                </div>
                            ))}
                            {form.imageUrls.length < 9 && (
                                <>
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        onChange={handleImageSelect}
                                        style={{ display: 'none' }}
                                    />
                                    <button
                                        type="button"
                                        onClick={() => fileInputRef.current?.click()}
                                        className="edit-image-add"
                                        disabled={uploadingIndex !== null}
                                    >
                                        {uploadingIndex !== null ? (
                                            <Loader2 size={24} className="animate-spin" />
                                        ) : (
                                            <>
                                                <Camera size={24} />
                                                <span>添加图片</span>
                                            </>
                                        )}
                                    </button>
                                </>
                            )}
                        </div>
                    </div>

                    <div className="edit-form-section">
                        <div className="edit-section-header">
                            <div className="edit-section-icon">
                                <FileText size={18} />
                            </div>
                            <div className="edit-section-title">
                                <h3>基本信息</h3>
                            </div>
                        </div>
                        <div className="edit-form-fields">
                            <div className="edit-field-group">
                                <label className="edit-field-label" htmlFor="edit-product-name">
                                    商品名称 <span className="edit-required">*</span>
                                </label>
                                <input
                                    id="edit-product-name"
                                    type="text"
                                    className={`edit-input ${errors.name ? 'has-error' : ''}`}
                                    value={form.name}
                                    onChange={e => updateField('name', e.target.value)}
                                    maxLength={200}
                                    placeholder="请输入商品名称"
                                />
                                {errors.name && <span className="edit-error-text">{errors.name}</span>}
                            </div>

                            <div className="edit-field-group">
                                <label className="edit-field-label" htmlFor="edit-product-desc">商品描述</label>
                                <textarea
                                    id="edit-product-desc"
                                    rows={4}
                                    className="edit-textarea"
                                    value={form.description}
                                    onChange={e => updateField('description', e.target.value)}
                                    maxLength={2000}
                                    placeholder="详细描述商品特点、使用情况等..."
                                />
                            </div>
                        </div>
                    </div>

                    <div className="edit-form-section">
                        <div className="edit-section-header">
                            <div className="edit-section-icon">
                                <Tag size={18} />
                            </div>
                            <div className="edit-section-title">
                                <h3>价格与分类</h3>
                            </div>
                        </div>
                        <div className="edit-form-fields">
                            <div className="edit-field-row">
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-price">
                                        售价 (¥) <span className="edit-required">*</span>
                                    </label>
                                    <input
                                        id="edit-product-price"
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        className={`edit-input ${errors.price ? 'has-error' : ''}`}
                                        value={form.price}
                                        onChange={e => updateField('price', e.target.value)}
                                        placeholder="0.00"
                                    />
                                    {errors.price && <span className="edit-error-text">{errors.price}</span>}
                                </div>
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-original-price">原价 (¥)</label>
                                    <input
                                        id="edit-product-original-price"
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        className="edit-input"
                                        value={form.originalPrice}
                                        onChange={e => updateField('originalPrice', e.target.value)}
                                        placeholder="0.00"
                                    />
                                </div>
                            </div>

                            <div className="edit-field-row">
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-category">
                                        商品类别 <span className="edit-required">*</span>
                                    </label>
                                    <select
                                        id="edit-product-category"
                                        className={`edit-select ${errors.categoryId ? 'has-error' : ''}`}
                                        value={form.categoryId}
                                        onChange={e => updateField('categoryId', e.target.value)}
                                    >
                                        <option value="">请选择类别</option>
                                        {categories?.map(cat => (
                                            <option key={cat.id} value={cat.id}>{cat.name}</option>
                                        ))}
                                    </select>
                                    {errors.categoryId && <span className="edit-error-text">{errors.categoryId}</span>}
                                </div>
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-condition">
                                        新旧程度 <span className="edit-required">*</span>
                                    </label>
                                    <select
                                        id="edit-product-condition"
                                        className={`edit-select ${errors.conditionLevel ? 'has-error' : ''}`}
                                        value={form.conditionLevel}
                                        onChange={e => updateField('conditionLevel', e.target.value)}
                                    >
                                        <option value="">请选择</option>
                                        {Object.entries(CONDITION_LABEL_MAP).map(([code, label]) => (
                                            <option key={code} value={code}>{label}</option>
                                        ))}
                                    </select>
                                    {errors.conditionLevel && <span className="edit-error-text">{errors.conditionLevel}</span>}
                                </div>
                            </div>
                        </div>
                    </div>

                    <div className="edit-form-section">
                        <div className="edit-section-header">
                            <div className="edit-section-icon">
                                <Settings size={18} />
                            </div>
                            <div className="edit-section-title">
                                <h3>其他信息</h3>
                            </div>
                        </div>
                        <div className="edit-form-fields">
                            <div className="edit-field-row">
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-stock">库存数量</label>
                                    <input
                                        id="edit-product-stock"
                                        type="number"
                                        min="1"
                                        className="edit-input"
                                        value={form.stock}
                                        onChange={e => updateField('stock', e.target.value)}
                                    />
                                </div>
                                <div className="edit-field-group">
                                    <label className="edit-field-label" htmlFor="edit-product-contact">联系方式</label>
                                    <input
                                        id="edit-product-contact"
                                        type="text"
                                        className="edit-input"
                                        value={form.contactMethod}
                                        onChange={e => updateField('contactMethod', e.target.value)}
                                        maxLength={50}
                                        placeholder="微信/手机号"
                                    />
                                </div>
                            </div>

                            <div className="edit-field-group">
                                <label className="edit-field-label" htmlFor="edit-product-location">
                                    <MapPin size={14} style={{ marginRight: 4 }} />
                                    交易地点
                                </label>
                                <input
                                    id="edit-product-location"
                                    type="text"
                                    className="edit-input"
                                    value={form.location}
                                    onChange={e => updateField('location', e.target.value)}
                                    maxLength={100}
                                    placeholder="如：图书馆门口、食堂等"
                                />
                            </div>
                        </div>
                    </div>

                    {updateProduct.isError && (
                        <div className="edit-submit-error">
                            <AlertTriangle size={18} />
                            更新失败，请稍后重试
                        </div>
                    )}

                    <div className="edit-form-actions">
                        <button className="edit-btn edit-btn-secondary" onClick={() => navigate(-1)}>
                            取消
                        </button>
                        <button className="edit-btn edit-btn-primary" onClick={handleSubmit} disabled={isSubmitting}>
                            {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : '保存修改'}
                        </button>
                    </div>
                </div>

                <div className="edit-danger-zone">
                    <div className="edit-danger-header">
                        <AlertTriangle size={18} />
                        <span>危险操作</span>
                    </div>
                    <p>删除商品后数据将无法恢复，请谨慎操作</p>
                    <button className="edit-delete-btn" onClick={() => setShowDeleteConfirm(true)}>
                        <Trash2 size={16} />
                        删除商品
                    </button>
                </div>
            </div>

            {showDeleteConfirm && (
                <div className="edit-modal-overlay">
                    <div className="edit-modal">
                        <div className="edit-modal-icon">
                            <AlertTriangle size={32} />
                        </div>
                        <h3>确认删除</h3>
                        <p>确定要删除这个商品吗？此操作不可撤销。</p>
                        <div className="edit-modal-actions">
                            <button className="edit-btn edit-btn-secondary" onClick={() => setShowDeleteConfirm(false)}>
                                取消
                            </button>
                            <button className="edit-btn edit-btn-danger" onClick={handleDelete} disabled={deleteProduct.isPending}>
                                {deleteProduct.isPending ? <Loader2 size={18} className="animate-spin" /> : '确认删除'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}

export default EditProductPage;
