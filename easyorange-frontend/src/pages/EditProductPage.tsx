import { useState, useEffect, useRef } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { Camera, X, Loader2 } from 'lucide-react';
import { useProduct, useUpdateProduct, useDeleteProduct, useCategories } from '@/hooks';
import { uploadFile } from '@/api/uploadApi';
import { compressImage } from '@/utils/imageCompress';
import { CONDITION_LABEL_MAP } from '@/types';
import '@/styles/main.css';

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

export function EditProductPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { data: product, isLoading: isLoadingProduct } = useProduct(Number(id));
    const updateProduct = useUpdateProduct(Number(id));
    const deleteProduct = useDeleteProduct();
    const { data: categories } = useCategories();
    const [form, setForm] = useState<FormState>({
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
    });
    const [errors, setErrors] = useState<FormErrors>({});
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    useEffect(() => {
        if (product) {
            setForm({
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
            });
        }
    }, [product]);

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
            } catch {
                // upload failed, skip
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
            // error handled by mutation state
        }
    };

    const handleDelete = async () => {
        try {
            await deleteProduct.mutateAsync(Number(id));
            navigate('/products');
        } catch {
            // error handled by mutation state
        }
    };

    if (isLoadingProduct) {
        return (
            <div className="loading-container">
                <div className="loading-spinner-lg"></div>
                <span className="loading-text">加载中...</span>
            </div>
        );
    }

    if (!product) {
        return (
            <div className="empty-state-container">
                <div className="empty-state-icon">📦</div>
                <p className="empty-state-text">商品不存在</p>
            </div>
        );
    }

    const isSubmitting = updateProduct.isPending;

    return (
        <div className="container py-6">
            <div className="mb-6">
                <h1 className="page-title-lg">编辑商品</h1>
                <p className="page-subtitle">修改商品信息</p>
            </div>

            <div className="card-elevated">
                <div className="mb-6">
                    <label className="form-label">商品图片</label>
                    <div className="flex flex-wrap gap-3">
                        {form.imageUrls.map((url, index) => (
                            <div key={index} className="relative" style={{ width: 100, height: 100 }}>
                                <img src={url} alt={`商品图片 ${index + 1}`} className="w-full h-full object-cover rounded-lg" />
                                <button
                                    type="button"
                                    onClick={() => handleImageUrlRemove(index)}
                                    className="absolute -top-2 -right-2 w-5 h-5 rounded-full flex items-center justify-center"
                                    style={{ background: 'var(--color-danger)', color: '#fff', fontSize: 12 }}
                                >
                                    <X size={12} />
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
                                    className="upload-zone"
                                    style={{ width: 100, height: 100, minHeight: 'auto' }}
                                    disabled={uploadingIndex !== null}
                                >
                                    <div className="upload-content" style={{ padding: 0 }}>
                                        {uploadingIndex !== null ? (
                                            <Loader2 size={24} className="animate-spin" />
                                        ) : (
                                            <>
                                                <Camera size={24} />
                                                <span className="upload-text" style={{ fontSize: '0.7rem' }}>添加图片</span>
                                            </>
                                        )}
                                    </div>
                                </button>
                            </>
                        )}
                    </div>
                </div>

                <div className="mb-6">
                    <label className="form-label">商品名称</label>
                    <input type="text" className={`form-input ${errors.name ? 'is-error' : ''}`} value={form.name} onChange={e => updateField('name', e.target.value)} maxLength={200} />
                    {errors.name && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.name}</p>}
                </div>

                <div className="mb-6">
                    <label className="form-label">商品描述</label>
                    <textarea rows={4} className="form-textarea" value={form.description} onChange={e => updateField('description', e.target.value)} maxLength={2000} />
                </div>

                <div className="mb-6 grid grid-cols-2 gap-4">
                    <div>
                        <label className="form-label">价格 (¥)</label>
                        <input type="number" step="0.01" min="0.01" className={`form-input ${errors.price ? 'is-error' : ''}`} value={form.price} onChange={e => updateField('price', e.target.value)} />
                        {errors.price && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.price}</p>}
                    </div>
                    <div>
                        <label className="form-label">原价 (¥)</label>
                        <input type="number" step="0.01" min="0.01" className="form-input" value={form.originalPrice} onChange={e => updateField('originalPrice', e.target.value)} />
                    </div>
                </div>

                <div className="mb-6 grid grid-cols-2 gap-4">
                    <div>
                        <label className="form-label">商品类别</label>
                        <select className={`form-input ${errors.categoryId ? 'is-error' : ''}`} value={form.categoryId} onChange={e => updateField('categoryId', e.target.value)}>
                            <option value="">请选择类别</option>
                            {categories?.map(cat => (
                                <option key={cat.id} value={cat.id}>{cat.name}</option>
                            ))}
                        </select>
                        {errors.categoryId && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.categoryId}</p>}
                    </div>
                    <div>
                        <label className="form-label">新旧程度</label>
                        <select className={`form-input ${errors.conditionLevel ? 'is-error' : ''}`} value={form.conditionLevel} onChange={e => updateField('conditionLevel', e.target.value)}>
                            <option value="">请选择</option>
                            {Object.entries(CONDITION_LABEL_MAP).map(([code, label]) => (
                                <option key={code} value={code}>{label}</option>
                            ))}
                        </select>
                        {errors.conditionLevel && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.conditionLevel}</p>}
                    </div>
                </div>

                <div className="mb-6 grid grid-cols-2 gap-4">
                    <div>
                        <label className="form-label">库存数量</label>
                        <input type="number" min="1" className="form-input" value={form.stock} onChange={e => updateField('stock', e.target.value)} />
                    </div>
                    <div>
                        <label className="form-label">联系方式</label>
                        <input type="text" className="form-input" value={form.contactMethod} onChange={e => updateField('contactMethod', e.target.value)} maxLength={50} />
                    </div>
                </div>

                <div className="mb-6">
                    <label className="form-label">交易地点</label>
                    <input type="text" className="form-input" value={form.location} onChange={e => updateField('location', e.target.value)} maxLength={100} />
                </div>

                {updateProduct.isError && (
                    <p style={{ color: 'var(--color-danger)', fontSize: '0.85rem', marginTop: 12 }}>更新失败，请稍后重试</p>
                )}

                <div className="mt-6 flex gap-3">
                    <button className="btn btn-primary flex-1" onClick={handleSubmit} disabled={isSubmitting}>
                        {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : '保存修改'}
                    </button>
                    <button className="btn btn-secondary flex-1" onClick={() => navigate(-1)}>
                        取消
                    </button>
                </div>
            </div>

            <div className="card-elevated mt-4" style={{ borderColor: 'var(--color-danger)', borderWidth: 1 }}>
                <div className="flex items-center justify-between">
                    <div>
                        <h3 style={{ color: 'var(--color-danger)', fontWeight: 600 }}>删除商品</h3>
                        <p style={{ fontSize: '0.85rem', color: 'var(--text-secondary)' }}>删除后无法恢复，请谨慎操作</p>
                    </div>
                    <button className="btn btn-ghost" style={{ color: 'var(--color-danger)' }} onClick={() => setShowDeleteConfirm(true)}>
                        删除商品
                    </button>
                </div>
            </div>

            {showDeleteConfirm && (
                <div style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.5)', display: 'flex', alignItems: 'center', justifyContent: 'center', zIndex: 999 }}>
                    <div className="card-elevated" style={{ maxWidth: 400, width: '90%' }}>
                        <h3 style={{ fontWeight: 600, marginBottom: 8 }}>确认删除</h3>
                        <p style={{ color: 'var(--text-secondary)', marginBottom: 16 }}>确定要删除这个商品吗？此操作不可撤销。</p>
                        <div className="flex gap-3">
                            <button className="btn btn-ghost flex-1" onClick={() => setShowDeleteConfirm(false)}>取消</button>
                            <button className="btn flex-1" style={{ background: 'var(--color-danger)', color: '#fff' }} onClick={handleDelete} disabled={deleteProduct.isPending}>
                                {deleteProduct.isPending ? <Loader2 size={18} className="animate-spin" /> : '确认删除'}
                            </button>
                        </div>
                    </div>
                </div>
            )}
        </div>
    );
}
