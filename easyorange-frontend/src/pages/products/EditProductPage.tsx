import { useState, useRef, useEffect } from 'react';
import { useNavigate, useParams } from 'react-router-dom';
import { useForm, Controller } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { Camera, X, Loader2, ArrowLeft, Package, Tag, MapPin, FileText, Settings, Trash2, AlertTriangle, Sparkles, Brain } from 'lucide-react';
import { useProduct, useUpdateProduct, useDeleteProduct, useCategories } from '@/hooks';
import { uploadFile } from '@/api/uploadApi';
import { compressImage } from '@/utils/imageCompress';
import { CONDITION_LABEL_MAP } from '@/constants';
import { ConfirmModal } from '@/admin/components/ConfirmModal';
import { Button } from '@/components/ui/button';
import { Input, Label } from '@/components/ui';
import { Textarea } from '@/components/ui/textarea';
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from '@/components/ui/select';
import { publishSchema, type PublishFormData } from '@/schemas/publishSchema';
import './edit-product.css';

function EditProductPage() {
    const { id } = useParams<{ id: string }>();
    const navigate = useNavigate();
    const { data: product, isLoading: isLoadingProduct } = useProduct(id ?? '');
    const updateProduct = useUpdateProduct(id ?? '');
    const deleteProduct = useDeleteProduct();
    const { data: categories } = useCategories();
    const [showDeleteConfirm, setShowDeleteConfirm] = useState(false);
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const fileInputRef = useRef<HTMLInputElement>(null);

    const {
        register,
        handleSubmit: rhfHandleSubmit,
        watch,
        setValue,
        control,
        reset,
        formState,
    } = useForm<PublishFormData>({
        resolver: zodResolver(publishSchema),
        reValidateMode: 'onChange',
        defaultValues: {
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
        },
    });

    const vals = watch();

    useEffect(() => {
        if (product) {
            reset({
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
    }, [product, reset]);

    const handleImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) {return;}

        for (const file of Array.from(files)) {
            const currentImages = watch('imageUrls');
            if (currentImages.length >= 9) {break;}
            if (!file.type.startsWith('image/')) {continue;}
            if (file.size > 10 * 1024 * 1024) {continue;}

            const index = currentImages.length;
            setUploadingIndex(index);
            try {
                const compressed = await compressImage(file);
                const result = await uploadFile(compressed);
                if (result.data?.url) {
                    setValue('imageUrls', [...watch('imageUrls'), result.data.url], { shouldValidate: true });
                }
            } catch {
                // image upload failed silently - error handled by parent
            } finally {
                setUploadingIndex(null);
            }
        }

        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const handleImageUrlRemove = (index: number) => {
        setValue('imageUrls', watch('imageUrls').filter((_, i) => i !== index), { shouldValidate: true });
    };

    const onSubmit = rhfHandleSubmit(async (data) => {
        try {
            await updateProduct.mutateAsync({
                name: data.name.trim(),
                description: data.description.trim(),
                price: Number(data.price),
                originalPrice: data.originalPrice ? Number(data.originalPrice) : undefined,
                categoryId: Number(data.categoryId),
                conditionLevel: Number(data.conditionLevel),
                stock: Number(data.stock) || 1,
                location: data.location.trim() || undefined,
                contactMethod: data.contactMethod.trim() || undefined,
                imageUrls: data.imageUrls,
            });
            navigate(`/products/${id}`);
        } catch {
            // update failed silently - error toast handled by hook
        }
    });

    const handleDelete = async () => {
        try {
            await deleteProduct.mutateAsync(id ?? '');
            navigate('/products');
        } catch {
            // delete failed
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
                <Button className="edit-empty-btn" onClick={() => navigate('/products')}>
                    返回商品列表
                </Button>
            </div>
        );
    }

    const isSubmitting = formState.isSubmitting || updateProduct.isPending;

    return (
        <div className="edit-product-page">
            <div className="edit-product-bg">
                <div className="edit-orb edit-orb-1"></div>
                <div className="edit-orb edit-orb-2"></div>
            </div>

            <div className="edit-product-nav">
                <Button variant="ghost" size="icon" onClick={() => navigate(-1)} className="edit-back-btn">
                    <ArrowLeft size={20} />
                </Button>
                <h1 className="edit-nav-title">编辑商品</h1>
                <Button
                    variant="ghost"
                    size="icon"
                    className="edit-delete-btn-nav"
                    onClick={() => setShowDeleteConfirm(true)}
                >
                    <Trash2 size={18} />
                </Button>
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
                            {vals.imageUrls.map((url, index) => (
                                <div key={index} className={`edit-image-item ${index === 0 ? 'is-cover' : ''}`}>
                                    <img src={url} alt={`商品图片 ${index + 1}`} />
                                    {index === 0 && <span className="edit-cover-badge">封面</span>}
                                    <Button
                                        type="button"
                                        variant="ghost"
                                        size="icon"
                                        onClick={() => handleImageUrlRemove(index)}
                                        className="edit-image-remove"
                                    >
                                        <X size={14} />
                                    </Button>
                                </div>
                            ))}
                            {vals.imageUrls.length < 9 && (
                                <>
                                    <input
                                        ref={fileInputRef}
                                        type="file"
                                        accept="image/*"
                                        multiple
                                        onChange={handleImageSelect}
                                        style={{ display: 'none' }}
                                    />
                                    <Button
                                        type="button"
                                        variant="outline"
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
                                    </Button>
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
                                <Label className="edit-field-label" htmlFor="edit-product-name">
                                    商品名称 <span className="edit-required">*</span>
                                </Label>
                                <Input
                                    id="edit-product-name"
                                    type="text"
                                    className={`edit-input ${formState.errors.name?.message ? 'has-error' : ''}`}
                                    maxLength={200}
                                    placeholder="请输入商品名称"
                                    {...register('name')}
                                />
                                {formState.errors.name?.message && <span className="edit-error-text">{formState.errors.name.message}</span>}
                            </div>

                            <div className="edit-field-group">
                                <Label className="edit-field-label" htmlFor="edit-product-desc">商品描述</Label>
                                <Textarea
                                    id="edit-product-desc"
                                    rows={4}
                                    className="edit-textarea"
                                    maxLength={2000}
                                    placeholder="详细描述商品特点、使用情况等..."
                                    {...register('description')}
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
                                    <Label className="edit-field-label" htmlFor="edit-product-price">
                                        售价 (¥) <span className="edit-required">*</span>
                                    </Label>
                                    <Input
                                        id="edit-product-price"
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        className={`edit-input ${formState.errors.price?.message ? 'has-error' : ''}`}
                                        placeholder="0.00"
                                        {...register('price')}
                                    />
                                    {formState.errors.price?.message && <span className="edit-error-text">{formState.errors.price.message}</span>}
                                </div>
                                <div className="edit-field-group">
                                    <Label className="edit-field-label" htmlFor="edit-product-original-price">原价 (¥)</Label>
                                    <Input
                                        id="edit-product-original-price"
                                        type="number"
                                        step="0.01"
                                        min="0.01"
                                        className="edit-input"
                                        placeholder="0.00"
                                        {...register('originalPrice')}
                                    />
                                </div>
                            </div>

                            <div className="edit-field-row">
                                <div className="edit-field-group">
                                    <Label className="edit-field-label" htmlFor="edit-product-category">
                                        商品类别 <span className="edit-required">*</span>
                                    </Label>
                                    <Controller
                                        name="categoryId"
                                        control={control}
                                        render={({ field }) => (
                                            <Select
                                                value={field.value || '__empty__'}
                                                onValueChange={(value) => field.onChange(value === '__empty__' ? '' : value)}
                                            >
                                                <SelectTrigger id="edit-product-category" className={`edit-select ${formState.errors.categoryId?.message ? 'has-error' : ''}`}>
                                                    <SelectValue placeholder="请选择类别" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="__empty__">请选择类别</SelectItem>
                                                    {categories?.map(cat => (
                                                        <SelectItem key={cat.id} value={String(cat.id)}>{cat.name}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                    />
                                    {formState.errors.categoryId?.message && <span className="edit-error-text">{formState.errors.categoryId.message}</span>}
                                </div>
                                <div className="edit-field-group">
                                    <Label className="edit-field-label" htmlFor="edit-product-condition">
                                        新旧程度 <span className="edit-required">*</span>
                                    </Label>
                                    <Controller
                                        name="conditionLevel"
                                        control={control}
                                        render={({ field }) => (
                                            <Select
                                                value={field.value || '__empty__'}
                                                onValueChange={(value) => field.onChange(value === '__empty__' ? '' : value)}
                                            >
                                                <SelectTrigger id="edit-product-condition" className={`edit-select ${formState.errors.conditionLevel?.message ? 'has-error' : ''}`}>
                                                    <SelectValue placeholder="请选择" />
                                                </SelectTrigger>
                                                <SelectContent>
                                                    <SelectItem value="__empty__">请选择</SelectItem>
                                                    {Object.entries(CONDITION_LABEL_MAP).map(([code, label]) => (
                                                        <SelectItem key={code} value={code}>{label}</SelectItem>
                                                    ))}
                                                </SelectContent>
                                            </Select>
                                        )}
                                    />
                                    {formState.errors.conditionLevel?.message && <span className="edit-error-text">{formState.errors.conditionLevel.message}</span>}
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
                                    <Label className="edit-field-label" htmlFor="edit-product-stock">库存数量</Label>
                                    <Input
                                        id="edit-product-stock"
                                        type="number"
                                        min="1"
                                        className="edit-input"
                                        {...register('stock')}
                                    />
                                </div>
                                <div className="edit-field-group">
                                    <Label className="edit-field-label" htmlFor="edit-product-contact">联系方式</Label>
                                    <Input
                                        id="edit-product-contact"
                                        type="text"
                                        className="edit-input"
                                        maxLength={50}
                                        placeholder="微信/手机号"
                                        {...register('contactMethod')}
                                    />
                                </div>
                            </div>

                            <div className="edit-field-group">
                                <Label className="edit-field-label" htmlFor="edit-product-location">
                                    <MapPin size={14} style={{ marginRight: 4 }} />
                                    交易地点
                                </Label>
                                <Input
                                    id="edit-product-location"
                                    type="text"
                                    className="edit-input"
                                    maxLength={100}
                                    placeholder="如：图书馆门口、食堂等"
                                    {...register('location')}
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
                        <Button variant="outline" className="edit-btn edit-btn-secondary" onClick={() => navigate(-1)}>
                            取消
                        </Button>
                        <Button className="edit-btn edit-btn-primary" onClick={onSubmit} disabled={isSubmitting}>
                            {isSubmitting ? <Loader2 size={18} className="animate-spin" /> : '保存修改'}
                        </Button>
                    </div>
                </div>

                <div className="edit-danger-zone">
                    <div className="edit-danger-header">
                        <AlertTriangle size={18} />
                        <span>危险操作</span>
                    </div>
                    <p>删除商品后数据将无法恢复，请谨慎操作</p>
                    <Button variant="destructive" className="edit-delete-btn" onClick={() => setShowDeleteConfirm(true)}>
                        <Trash2 size={16} />
                        删除商品
                    </Button>
                </div>
            </div>

            <ConfirmModal
                isOpen={showDeleteConfirm}
                title="确认删除"
                content="确定要删除这个商品吗？此操作不可撤销。"
                confirmText="确认删除"
                cancelText="取消"
                variant="danger"
                isLoading={deleteProduct.isPending}
                onConfirm={handleDelete}
                onCancel={() => setShowDeleteConfirm(false)}
            />
        </div>
    );
}

export default EditProductPage;
