import { useState, useRef } from 'react';
import { useNavigate } from 'react-router-dom';
import { Camera, Info, X, Loader2 } from 'lucide-react';
import { useCreateProduct, useCategories } from '@/hooks';
import { uploadFile } from '@/api/uploadApi';
import { CONDITION_LABEL_MAP } from '@/types/product';
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
    imageUrls?: string;
}

const INITIAL_FORM: FormState = {
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

export function PublishPage() {
    const navigate = useNavigate();
    const createProduct = useCreateProduct();
    const { data: categories } = useCategories();
    const [form, setForm] = useState<FormState>(INITIAL_FORM);
    const [errors, setErrors] = useState<FormErrors>({});
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const validate = (): boolean => {
        const newErrors: FormErrors = {};
        if (!form.name.trim()) {
            newErrors.name = '请输入商品名称';
        }
        if (!form.price || Number(form.price) <= 0) {
            newErrors.price = '请输入有效价格';
        }
        if (!form.categoryId) {
            newErrors.categoryId = '请选择商品类别';
        }
        if (!form.conditionLevel) {
            newErrors.conditionLevel = '请选择新旧程度';
        }
        if (form.imageUrls.length === 0) {
            newErrors.imageUrls = '请至少上传一张图片';
        }
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
        if (!files) return;

        for (const file of Array.from(files)) {
            if (form.imageUrls.length >= 9) break;
            if (!file.type.startsWith('image/')) continue;
            if (file.size > 10 * 1024 * 1024) continue;

            const index = form.imageUrls.length;
            setUploadingIndex(index);
            try {
                const result = await uploadFile(file);
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

    const handleSubmit = async (isDraft: boolean) => {
        if (!isDraft && !validate()) return;

        const payload = {
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
        };

        try {
            const result = await createProduct.mutateAsync(payload);
            const productId = typeof result === 'object' && result !== null
                ? (result as Record<string, unknown>).id
                : result;
            navigate(`/products/${productId}`);
        } catch {
            // error handled by mutation state
        }
    };

    const isSubmitting = createProduct.isPending;

    return (
        <div className="container py-6">
            <div className="mb-6">
                <h1 className="page-title-lg">发布商品</h1>
                <p className="page-subtitle">快速发布您的二手物品</p>
            </div>

            <div className="card-elevated">
                <div className="mb-6">
                    <label className="form-label">商品图片 <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                    <div className="flex flex-wrap gap-3">
                        {form.imageUrls.map((url, index) => (
                            <div key={index} className="relative" style={{ width: 100, height: 100 }}>
                                <img
                                    src={url}
                                    alt={`商品图片 ${index + 1}`}
                                    className="w-full h-full object-cover rounded-lg"
                                />
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
                    {errors.imageUrls && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.imageUrls}</p>}
                </div>

                <div className="mb-6">
                    <label className="form-label">商品名称 <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                    <input
                        type="text"
                        placeholder="请输入商品名称"
                        className={`form-input ${errors.name ? 'is-error' : ''}`}
                        value={form.name}
                        onChange={e => updateField('name', e.target.value)}
                        maxLength={200}
                    />
                    {errors.name && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.name}</p>}
                </div>

                <div className="mb-6">
                    <label className="form-label">商品描述</label>
                    <textarea
                        rows={4}
                        placeholder="请详细描述您的商品，包括品牌、型号、新旧程度等信息"
                        className="form-textarea"
                        value={form.description}
                        onChange={e => updateField('description', e.target.value)}
                        maxLength={2000}
                    />
                </div>

                <div className="mb-6 grid grid-cols-2 gap-4">
                    <div>
                        <label className="form-label">价格 (¥) <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                        <input
                            type="number"
                            placeholder="0.00"
                            step="0.01"
                            min="0.01"
                            className={`form-input ${errors.price ? 'is-error' : ''}`}
                            value={form.price}
                            onChange={e => updateField('price', e.target.value)}
                        />
                        {errors.price && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.price}</p>}
                    </div>
                    <div>
                        <label className="form-label">原价 (¥)</label>
                        <input
                            type="number"
                            placeholder="0.00（选填）"
                            step="0.01"
                            min="0.01"
                            className="form-input"
                            value={form.originalPrice}
                            onChange={e => updateField('originalPrice', e.target.value)}
                        />
                    </div>
                </div>

                <div className="mb-6 grid grid-cols-2 gap-4">
                    <div>
                        <label className="form-label">商品类别 <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                        <select
                            className={`form-input ${errors.categoryId ? 'is-error' : ''}`}
                            value={form.categoryId}
                            onChange={e => updateField('categoryId', e.target.value)}
                        >
                            <option value="">请选择类别</option>
                            {categories?.map(cat => (
                                <option key={cat.id} value={cat.id}>{cat.name}</option>
                            ))}
                        </select>
                        {errors.categoryId && <p style={{ color: 'var(--color-danger)', fontSize: '0.8rem', marginTop: 4 }}>{errors.categoryId}</p>}
                    </div>
                    <div>
                        <label className="form-label">新旧程度 <span style={{ color: 'var(--color-danger)' }}>*</span></label>
                        <select
                            className={`form-input ${errors.conditionLevel ? 'is-error' : ''}`}
                            value={form.conditionLevel}
                            onChange={e => updateField('conditionLevel', e.target.value)}
                        >
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
                        <input
                            type="number"
                            placeholder="1"
                            min="1"
                            className="form-input"
                            value={form.stock}
                            onChange={e => updateField('stock', e.target.value)}
                        />
                    </div>
                    <div>
                        <label className="form-label">联系方式</label>
                        <input
                            type="text"
                            placeholder="微信号/QQ号"
                            className="form-input"
                            value={form.contactMethod}
                            onChange={e => updateField('contactMethod', e.target.value)}
                            maxLength={50}
                        />
                    </div>
                </div>

                <div className="mb-6">
                    <label className="form-label">交易地点</label>
                    <input
                        type="text"
                        placeholder="如：清水河校区南门"
                        className="form-input"
                        value={form.location}
                        onChange={e => updateField('location', e.target.value)}
                        maxLength={100}
                    />
                </div>

                <div className="info-banner">
                    <div className="info-banner-icon">
                        <Info size={18} />
                    </div>
                    <p>发布前请确保商品信息真实有效，定价合理。禁止发布违规商品。</p>
                </div>

                {createProduct.isError && (
                    <p style={{ color: 'var(--color-danger)', fontSize: '0.85rem', marginTop: 12 }}>
                        发布失败，请稍后重试
                    </p>
                )}

                <div className="mt-6 flex gap-3">
                    <button
                        className="btn btn-primary flex-1"
                        onClick={() => handleSubmit(false)}
                        disabled={isSubmitting}
                    >
                        {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : '发布商品'}
                    </button>
                    <button
                        className="btn btn-secondary flex-1"
                        onClick={() => handleSubmit(true)}
                        disabled={isSubmitting}
                    >
                        保存草稿
                    </button>
                </div>
            </div>
        </div>
    );
}
