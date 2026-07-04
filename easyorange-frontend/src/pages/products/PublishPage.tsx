import { zodResolver } from '@hookform/resolvers/zod';
import {
    AlertCircle,
    Camera,
    Check,
    ChevronRight,
    DollarSign,
    FileText,
    GripVertical,
    ImageIcon,
    Info,
    Loader2,
    MapPin,
    MessageCircle,
    Package,
    Sparkles,
    Tag,
    Trash2,
    Upload,
} from 'lucide-react';
import { useCallback, useEffect, useRef, useState } from 'react';
import { Controller, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { productApi } from '@/api/productApi';
import { uploadFile } from '@/api/uploadApi';
import { AiCopyGeneration } from '@/components/ai/AiCopyGeneration';
import { AiPhotoCapture } from '@/components/ai/AiPhotoCapture';
import { AiPricingBadge } from '@/components/ai/AiPricingBadge';
import { Input, Label } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { Textarea } from '@/components/ui/textarea';
import { CONDITION_LABEL_MAP } from '@/constants';
import { useCategories, useCreateProduct } from '@/hooks';
import { useAiCopyGeneration } from '@/hooks/useAiCopyGeneration';
import { useAiPricing } from '@/hooks/useAiPricing';
import { useAutoListing } from '@/hooks/useAutoListing';
import { type PublishFormData, publishSchema } from '@/schemas/publishSchema';
import { useUIStore } from '@/store/uiStore';
import { compressImage } from '@/utils/imageCompress';
import './publish.css';

const CONDITION_ICONS: Record<number, string> = {
    1: '✨',
    2: '🌟',
    3: '👍',
    4: '🔧',
};

const CONDITION_DESC: Record<number, string> = {
    1: '未拆封，全新状态',
    2: '使用过几次，几乎无痕迹',
    3: '有正常使用痕迹',
    4: '有明显使用痕迹，功能正常',
};

function PublishPage() {
    const navigate = useNavigate();
    const createProduct = useCreateProduct();
    const { data: categories } = useCategories();
    const addToast = useUIStore(s => s.addToast);
    const { suggestion, isLoading: aiPricingLoading, getPricing, clearSuggestion } = useAiPricing();
    const {
        result: autoListingResult,
        isLoading: autoListingLoading,
        analyzeImages,
        clearResult: clearAutoListing,
    } = useAutoListing();
    const { result: copyResult, isLoading: copyLoading, generateCopy, clearResult: clearCopy } = useAiCopyGeneration();
    const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
    const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
    const [isDragging, setIsDragging] = useState(false);
    const [activeSection, setActiveSection] = useState(0);
    const fileInputRef = useRef<HTMLInputElement>(null);
    const dragItemRef = useRef<number | null>(null);
    const pageRef = useRef<HTMLDivElement>(null);

    const {
        register,
        handleSubmit: rhfHandleSubmit,
        watch,
        setValue,
        control,
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
        const timer = setTimeout(() => {
            if (pageRef.current) {
                pageRef.current.classList.add('page-ready');
            }
        }, 100);
        return () => clearTimeout(timer);
    }, []);

    useEffect(() => {
        if (autoListingResult) {
            setValue('name', autoListingResult.title);
            setValue('description', autoListingResult.description);
            if (autoListingResult.price > 0) {
                setValue('price', String(autoListingResult.price));
            }
            if (autoListingResult.conditionLevel > 0) {
                setValue('conditionLevel', String(autoListingResult.conditionLevel));
            }
            if (autoListingResult.location) {
                setValue('location', autoListingResult.location);
            }
            if (autoListingResult.categoryName) {
                const category = categories?.find(c => c.name === autoListingResult.categoryName);
                if (category) {
                    setValue('categoryId', String(category.id));
                }
            }
            clearAutoListing();
        }
    }, [autoListingResult, categories, setValue, clearAutoListing]);

    const handleImageSelect = async (e: React.ChangeEvent<HTMLInputElement>) => {
        const files = e.target.files;
        if (!files) {
            return;
        }
        await processFiles(Array.from(files));
        if (fileInputRef.current) {
            fileInputRef.current.value = '';
        }
    };

    const processFiles = async (files: File[]) => {
        for (const file of files) {
            const currentImages = watch('imageUrls');
            if (currentImages.length >= 9) {
                break;
            }
            if (!file.type.startsWith('image/')) {
                continue;
            }
            if (file.size > 10 * 1024 * 1024) {
                continue;
            }

            const index = currentImages.length;
            setUploadingIndex(index);
            try {
                const compressed = await compressImage(file);
                const result = await uploadFile(compressed);
                if (result.data?.url) {
                    setValue('imageUrls', [...watch('imageUrls'), result.data.url], { shouldValidate: true });
                }
            } catch {
                addToast({ type: 'error', message: '图片上传失败，请重试' });
            } finally {
                setUploadingIndex(null);
            }
        }
    };

    const handleImageUrlRemove = (index: number) => {
        setValue(
            'imageUrls',
            watch('imageUrls').filter((_, i) => i !== index),
            { shouldValidate: true }
        );
    };

    const handleDragStart = (index: number) => {
        dragItemRef.current = index;
    };

    const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
        e.preventDefault();
        setDragOverIndex(index);
    }, []);

    const handleDrop = useCallback(
        (e: React.DragEvent, index: number) => {
            e.preventDefault();
            setDragOverIndex(null);
            const draggedIndex = dragItemRef.current;
            if (draggedIndex === null || draggedIndex === index) {
                return;
            }

            const newUrls = [...watch('imageUrls')];
            const [removed] = newUrls.splice(draggedIndex, 1);
            newUrls.splice(index, 0, removed);
            setValue('imageUrls', newUrls);
            dragItemRef.current = null;
        },
        [watch, setValue]
    );

    const handleDragEnd = () => {
        dragItemRef.current = null;
        setDragOverIndex(null);
    };

    const handleDragEnter = (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(true);
    };

    const handleDragLeave = (e: React.DragEvent) => {
        e.preventDefault();
        if (!e.currentTarget.contains(e.relatedTarget as Node)) {
            setIsDragging(false);
        }
    };

    const handleDropZoneDrop = async (e: React.DragEvent) => {
        e.preventDefault();
        setIsDragging(false);
        const files = e.dataTransfer.files;
        if (files) {
            await processFiles(Array.from(files));
        }
    };

    const handleFormSubmit = async (data: PublishFormData, isDraft: boolean) => {
        const payload = {
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
        };

        try {
            const productId = (await createProduct.mutateAsync(payload)) as string;

            if (!isDraft && productId) {
                await productApi.goOnline(productId);
            }

            navigate(`/products/${productId}`);
        } catch {
            // error handled by mutation state
        }
    };

    const onSubmitDraft = rhfHandleSubmit(data => handleFormSubmit(data, true));
    const onSubmitPublish = rhfHandleSubmit(data => handleFormSubmit(data, false));

    const isSubmitting = formState.isSubmitting || createProduct.isPending;
    const progress = Math.min(
        100,
        Math.round(
            (((vals.imageUrls.length > 0 ? 1 : 0) +
                (vals.name ? 1 : 0) +
                (vals.price ? 1 : 0) +
                (vals.categoryId ? 1 : 0) +
                (vals.conditionLevel ? 1 : 0)) /
                5) *
                100
        )
    );

    const sections = [
        { id: 'images', label: '资产图片', icon: ImageIcon },
        { id: 'basic', label: '基本信息', icon: Tag },
        { id: 'detail', label: '详细信息', icon: FileText },
        { id: 'price', label: '价格库存', icon: DollarSign },
    ];

    return (
        <div ref={pageRef} className="publish-page-v2">
            {/* Background Effects */}
            <div className="publish-bg-v2">
                <div className="bg-gradient-layer-v2" />
                <div className="bg-noise-v2" />
                <div className="floating-orbs-v2">
                    <div className="orb-v2 orb-v2-1" />
                    <div className="orb-v2 orb-v2-2" />
                    <div className="orb-v2 orb-v2-3" />
                </div>
            </div>

            <div className="publish-main-v2">
                <div className="publish-container-v2">
                    {/* Header Section */}
                    <div className="publish-header-v2">
                        <div className="header-badge">
                            <Sparkles size={14} />
                            <span>提交您的资产</span>
                        </div>
                        <h1 className="page-title-v2">
                            <span className="gradient-text">提交资产</span>
                        </h1>
                        <p className="page-subtitle-v2">填写信息，让 AI 帮你智能托管发布</p>
                    </div>

                    {/* Progress Bar */}
                    <div className="progress-section-v2">
                        <div className="progress-header">
                            <span className="progress-label">完成度</span>
                            <span className="progress-value">{progress}%</span>
                        </div>
                        <div className="progress-bar-v2">
                            <div className="progress-fill-v2" style={{ width: `${progress}%` }} />
                        </div>
                        <div className="progress-steps-v2">
                            {sections.map((section, index) => {
                                const Icon = section.icon;
                                const isActive = index <= activeSection;
                                const isCompleted =
                                    index < activeSection ||
                                    (index === 0 && vals.imageUrls.length > 0) ||
                                    (index === 1 && vals.name && vals.categoryId && vals.conditionLevel) ||
                                    (index === 2 && vals.description) ||
                                    (index === 3 && vals.price);
                                return (
                                    <Button
                                        key={section.id}
                                        variant="ghost"
                                        className={`progress-step-v2 ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
                                        onClick={() => {
                                            setActiveSection(index);
                                            document
                                                .getElementById(section.id)
                                                ?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                                        }}
                                    >
                                        <div className="step-icon-v2">
                                            {isCompleted ? <Check size={14} /> : <Icon size={14} />}
                                        </div>
                                        <span className="step-label-v2">{section.label}</span>
                                    </Button>
                                );
                            })}
                        </div>
                    </div>

                    {/* Form Card */}
                    <div className="publish-form-v2">
                        {/* Image Upload Section */}
                        <section className="form-section-v2" id="images">
                            <div className="section-header-v2">
                                <div className="section-icon-v2">
                                    <ImageIcon size={20} />
                                </div>
                                <div className="section-title-group">
                                    <h2 className="section-title-v2">资产图片</h2>
                                    <span className="section-hint-v2">最多9张，首张为封面</span>
                                </div>
                                <span className="required-badge">必填</span>
                            </div>

                            <div
                                className={`upload-zone-v2 ${isDragging ? 'dragover' : ''} ${formState.errors.imageUrls?.message ? 'has-error' : ''}`}
                                role="region"
                                aria-label="图片上传区域"
                                onDragEnter={handleDragEnter}
                                onDragOver={handleDragEnter}
                                onDragLeave={handleDragLeave}
                                onDrop={handleDropZoneDrop}
                            >
                                <input
                                    ref={fileInputRef}
                                    type="file"
                                    accept="image/*"
                                    multiple
                                    onChange={handleImageSelect}
                                    style={{ display: 'none' }}
                                />
                                {vals.imageUrls.length === 0 ? (
                                    <Button
                                        type="button"
                                        variant="outline"
                                        onClick={() => fileInputRef.current?.click()}
                                        className="upload-trigger-v2"
                                        disabled={uploadingIndex !== null}
                                    >
                                        <div className="upload-icon-v2">
                                            {uploadingIndex !== null ? (
                                                <Loader2 size={32} className="animate-spin" />
                                            ) : (
                                                <Camera size={32} />
                                            )}
                                        </div>
                                        <div className="upload-text-v2">
                                            <span className="upload-title">点击或拖拽上传图片</span>
                                            <span className="upload-desc">
                                                支持 JPG、PNG、WEBP 格式，单张不超过 10MB
                                            </span>
                                        </div>
                                    </Button>
                                ) : (
                                    <div className="image-grid-v2">
                                        {vals.imageUrls.map((url, index) => (
                                            <div
                                                key={`${url}-${index}`}
                                                className={`image-item-v2 ${dragOverIndex === index ? 'drag-over' : ''} ${index === 0 ? 'is-cover' : ''}`}
                                                role="button"
                                                tabIndex={-1}
                                                draggable
                                                onDragStart={() => handleDragStart(index)}
                                                onDragOver={e => handleDragOver(e, index)}
                                                onDrop={e => handleDrop(e, index)}
                                                onDragEnd={handleDragEnd}
                                                onKeyDown={() => {}}
                                            >
                                                <img src={url} alt={`资产图片 ${index + 1}`} width="120" height="120" />
                                                {index === 0 && (
                                                    <div className="cover-badge-v2">
                                                        <span>封面</span>
                                                    </div>
                                                )}
                                                <div className="image-actions-v2">
                                                    <Button
                                                        type="button"
                                                        variant="ghost"
                                                        size="icon"
                                                        className="image-action-btn"
                                                        onClick={() => handleImageUrlRemove(index)}
                                                        title="删除"
                                                    >
                                                        <Trash2 size={14} />
                                                    </Button>
                                                </div>
                                                <div className="drag-handle-v2">
                                                    <GripVertical size={14} />
                                                </div>
                                            </div>
                                        ))}
                                        {vals.imageUrls.length < 9 && (
                                            <Button
                                                type="button"
                                                variant="outline"
                                                onClick={() => fileInputRef.current?.click()}
                                                className="add-more-v2"
                                                disabled={uploadingIndex !== null}
                                            >
                                                {uploadingIndex !== null ? (
                                                    <Loader2 size={24} className="animate-spin" />
                                                ) : (
                                                    <>
                                                        <Upload size={24} />
                                                        <span>添加图片</span>
                                                    </>
                                                )}
                                            </Button>
                                        )}
                                    </div>
                                )}
                                {vals.imageUrls.length > 0 && (
                                    <AiPhotoCapture
                                        onAnalyze={() => analyzeImages(vals.imageUrls)}
                                        isLoading={autoListingLoading}
                                        hasImages={vals.imageUrls.length > 0}
                                    />
                                )}
                            </div>
                            {formState.errors.imageUrls?.message && (
                                <div className="error-message-v2">
                                    <AlertCircle size={14} />
                                    <span>{formState.errors.imageUrls.message}</span>
                                </div>
                            )}
                        </section>

                        {/* Basic Info Section */}
                        <section className="form-section-v2" id="basic">
                            <div className="section-header-v2">
                                <div className="section-icon-v2">
                                    <Tag size={20} />
                                </div>
                                <div className="section-title-group">
                                    <h2 className="section-title-v2">基本信息</h2>
                                    <span className="section-hint-v2">填写资产的核心信息</span>
                                </div>
                            </div>

                            <div className="form-fields-v2">
                                <div className="field-group-v2">
                                    <Label className="field-label-v2" htmlFor="name">
                                        资产名称
                                        <span className="required-mark">*</span>
                                    </Label>
                                    <div
                                        className={`input-wrapper-v2 ${formState.errors.name?.message ? 'has-error' : ''}`}
                                    >
                                        <Input
                                            id="name"
                                            type="text"
                                            placeholder="给资产起个吸引人的名字"
                                            maxLength={200}
                                            className="field-input-v2"
                                            {...register('name')}
                                        />
                                        <span className="char-count-v2">{vals.name.length}/200</span>
                                    </div>
                                    {formState.errors.name?.message && (
                                        <div className="error-message-v2">
                                            <AlertCircle size={14} />
                                            <span>{formState.errors.name.message}</span>
                                        </div>
                                    )}
                                </div>

                                <div className="field-row-v2">
                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="categoryId">
                                            资产类别
                                            <span className="required-mark">*</span>
                                        </Label>
                                        <div
                                            className={`select-wrapper-v2 ${formState.errors.categoryId?.message ? 'has-error' : ''}`}
                                        >
                                            <Controller
                                                name="categoryId"
                                                control={control}
                                                render={({ field }) => (
                                                    <Select
                                                        value={field.value || '__empty__'}
                                                        onValueChange={value =>
                                                            field.onChange(value === '__empty__' ? '' : value)
                                                        }
                                                    >
                                                        <SelectTrigger id="categoryId" className="field-select-v2">
                                                            <SelectValue placeholder="选择类别" />
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectItem value="__empty__">选择类别</SelectItem>
                                                            {categories?.map(cat => (
                                                                <SelectItem key={cat.id} value={String(cat.id)}>
                                                                    {cat.name}
                                                                </SelectItem>
                                                            ))}
                                                        </SelectContent>
                                                    </Select>
                                                )}
                                            />
                                            <ChevronRight size={16} className="select-arrow" />
                                        </div>
                                        {formState.errors.categoryId?.message && (
                                            <div className="error-message-v2">
                                                <AlertCircle size={14} />
                                                <span>{formState.errors.categoryId.message}</span>
                                            </div>
                                        )}
                                    </div>

                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="conditionLevel">
                                            新旧程度
                                            <span className="required-mark">*</span>
                                        </Label>
                                        <div
                                            className={`select-wrapper-v2 ${formState.errors.conditionLevel?.message ? 'has-error' : ''}`}
                                        >
                                            <Controller
                                                name="conditionLevel"
                                                control={control}
                                                render={({ field }) => (
                                                    <Select
                                                        value={field.value || '__empty__'}
                                                        onValueChange={value =>
                                                            field.onChange(value === '__empty__' ? '' : value)
                                                        }
                                                    >
                                                        <SelectTrigger id="conditionLevel" className="field-select-v2">
                                                            <SelectValue placeholder="选择成色" />
                                                        </SelectTrigger>
                                                        <SelectContent>
                                                            <SelectItem value="__empty__">选择成色</SelectItem>
                                                            {Object.entries(CONDITION_LABEL_MAP).map(
                                                                ([code, label]) => (
                                                                    <SelectItem key={code} value={code}>
                                                                        {label}
                                                                    </SelectItem>
                                                                )
                                                            )}
                                                        </SelectContent>
                                                    </Select>
                                                )}
                                            />
                                            <ChevronRight size={16} className="select-arrow" />
                                        </div>
                                        {formState.errors.conditionLevel?.message && (
                                            <div className="error-message-v2">
                                                <AlertCircle size={14} />
                                                <span>{formState.errors.conditionLevel.message}</span>
                                            </div>
                                        )}
                                    </div>
                                </div>

                                {vals.conditionLevel && (
                                    <div className="condition-preview">
                                        <div className="condition-icon-large">
                                            {CONDITION_ICONS[Number(vals.conditionLevel)]}
                                        </div>
                                        <div className="condition-info">
                                            <span className="condition-name">
                                                {CONDITION_LABEL_MAP[Number(vals.conditionLevel)]}
                                            </span>
                                            <span className="condition-desc">
                                                {CONDITION_DESC[Number(vals.conditionLevel)]}
                                            </span>
                                        </div>
                                    </div>
                                )}
                            </div>
                        </section>

                        {/* Detail Section */}
                        <section className="form-section-v2" id="detail">
                            <div className="section-header-v2">
                                <div className="section-icon-v2">
                                    <FileText size={20} />
                                </div>
                                <div className="section-title-group">
                                    <h2 className="section-title-v2">详细信息</h2>
                                    <span className="section-hint-v2">详细描述能提高流转率</span>
                                </div>
                            </div>

                            <div className="form-fields-v2">
                                <div className="field-group-v2">
                                    <Label className="field-label-v2" htmlFor="description">
                                        资产描述
                                    </Label>
                                    <div className="textarea-wrapper-v2">
                                        <Textarea
                                            id="description"
                                            rows={5}
                                            placeholder="详细描述资产的品牌、型号、规格、使用情况等信息，让认领方更了解您的资产..."
                                            maxLength={2000}
                                            className="field-textarea-v2"
                                            {...register('description')}
                                        />
                                        <span className="char-count-v2">{vals.description.length}/2000</span>
                                    </div>
                                    <AiCopyGeneration
                                        productName={vals.name}
                                        onGenerate={style => {
                                            const category = categories?.find(c => String(c.id) === vals.categoryId);
                                            generateCopy({
                                                productName: vals.name,
                                                categoryName: category?.name || undefined,
                                                conditionLevel: vals.conditionLevel
                                                    ? Number(vals.conditionLevel)
                                                    : undefined,
                                                originalPrice: vals.originalPrice || undefined,
                                                style,
                                            });
                                        }}
                                        onApply={result => {
                                            setValue('name', result.title);
                                            setValue('description', result.description);
                                            clearCopy();
                                        }}
                                        result={copyResult}
                                        isLoading={copyLoading}
                                    />
                                </div>

                                <div className="field-row-v2">
                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="location">
                                            <MapPin size={14} />
                                            交易地点
                                        </Label>
                                        <div className="input-wrapper-v2">
                                            <Input
                                                id="location"
                                                type="text"
                                                placeholder="如：清水河校区南门"
                                                maxLength={100}
                                                className="field-input-v2"
                                                {...register('location')}
                                            />
                                        </div>
                                    </div>

                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="contactMethod">
                                            <MessageCircle size={14} />
                                            联系方式
                                        </Label>
                                        <div className="input-wrapper-v2">
                                            <Input
                                                id="contactMethod"
                                                type="text"
                                                placeholder="微信号 / QQ号"
                                                maxLength={50}
                                                className="field-input-v2"
                                                {...register('contactMethod')}
                                            />
                                        </div>
                                    </div>
                                </div>
                            </div>
                        </section>

                        {/* Price Section */}
                        <section className="form-section-v2" id="price">
                            <div className="section-header-v2">
                                <div className="section-icon-v2">
                                    <DollarSign size={20} />
                                </div>
                                <div className="section-title-group">
                                    <h2 className="section-title-v2">价格库存</h2>
                                    <span className="section-hint-v2">合理定价更容易流转</span>
                                </div>
                            </div>

                            <div className="form-fields-v2">
                                <div className="field-row-v2">
                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="price">
                                            出售价格
                                            <span className="required-mark">*</span>
                                        </Label>
                                        <div
                                            className={`input-wrapper-v2 price-input-wrapper ${formState.errors.price?.message ? 'has-error' : ''}`}
                                        >
                                            <span className="price-symbol-v2">¥</span>
                                            <Input
                                                id="price"
                                                type="number"
                                                placeholder="0.00"
                                                step="0.01"
                                                min="0.01"
                                                className="field-input-v2 price-input"
                                                {...register('price')}
                                            />
                                        </div>
                                        {formState.errors.price?.message && (
                                            <div className="error-message-v2">
                                                <AlertCircle size={14} />
                                                <span>{formState.errors.price.message}</span>
                                            </div>
                                        )}
                                    </div>

                                    <div className="field-group-v2">
                                        <Label className="field-label-v2" htmlFor="originalPrice">
                                            原价（选填）
                                        </Label>
                                        <div className="input-wrapper-v2 price-input-wrapper">
                                            <span className="price-symbol-v2">¥</span>
                                            <Input
                                                id="originalPrice"
                                                type="number"
                                                placeholder="0.00"
                                                step="0.01"
                                                min="0.01"
                                                className="field-input-v2 price-input"
                                                {...register('originalPrice')}
                                            />
                                        </div>
                                    </div>
                                </div>

                                {vals.price &&
                                    vals.originalPrice &&
                                    Number(vals.originalPrice) > Number(vals.price) && (
                                        <div className="discount-tag">
                                            <span className="discount-badge">
                                                省{' '}
                                                {Math.round(
                                                    (1 - Number(vals.price) / Number(vals.originalPrice)) * 100
                                                )}
                                                %
                                            </span>
                                            <span className="discount-text">
                                                比原价优惠 ¥
                                                {(Number(vals.originalPrice) - Number(vals.price)).toFixed(2)}
                                            </span>
                                        </div>
                                    )}

                                {vals.name && !suggestion && (
                                    <Button
                                        className="ai-pricing-btn"
                                        onClick={() => {
                                            const category = categories?.find(c => String(c.id) === vals.categoryId);
                                            getPricing({
                                                productName: vals.name,
                                                description: vals.description || undefined,
                                                categoryName: category?.name || undefined,
                                                conditionLevel: vals.conditionLevel
                                                    ? Number(vals.conditionLevel)
                                                    : undefined,
                                                originalPrice: vals.originalPrice
                                                    ? Number(vals.originalPrice)
                                                    : undefined,
                                            });
                                        }}
                                        disabled={aiPricingLoading}
                                    >
                                        {aiPricingLoading ? (
                                            <>
                                                <Loader2 size={14} className="animate-spin" />
                                                AI 分析中...
                                            </>
                                        ) : (
                                            <>
                                                <Sparkles size={14} />
                                                AI 智能估值
                                            </>
                                        )}
                                    </Button>
                                )}

                                {suggestion && (
                                    <AiPricingBadge
                                        suggestion={suggestion}
                                        onApply={price => {
                                            setValue('price', String(price));
                                            clearSuggestion();
                                        }}
                                        isLoading={aiPricingLoading}
                                    />
                                )}

                                <div className="field-group-v2" style={{ maxWidth: '200px' }}>
                                    <Label className="field-label-v2" htmlFor="stock">
                                        <Package size={14} />
                                        库存数量
                                    </Label>
                                    <div className="input-wrapper-v2">
                                        <Input
                                            id="stock"
                                            type="number"
                                            placeholder="1"
                                            min="1"
                                            className="field-input-v2"
                                            {...register('stock')}
                                        />
                                        <span className="stock-unit">件</span>
                                    </div>
                                </div>
                            </div>
                        </section>

                        {/* Info Banner */}
                        <div className="info-banner-v2">
                            <Info size={18} />
                            <p>发布前请确保资产信息真实有效，定价合理。禁止发布违规资产。</p>
                        </div>

                        {/* Error Message */}
                        {createProduct.isError && (
                            <div className="submit-error-v2">
                                <AlertCircle size={16} />
                                <span>发布失败，请稍后重试</span>
                            </div>
                        )}

                        {/* Actions */}
                        <div className="form-actions-v2">
                            <Button
                                variant="outline"
                                className="btn-draft-v2"
                                onClick={onSubmitDraft}
                                disabled={isSubmitting}
                            >
                                保存草稿
                            </Button>
                            <Button className="btn-publish-v2" onClick={onSubmitPublish} disabled={isSubmitting}>
                                {isSubmitting ? (
                                    <>
                                        <Loader2 size={18} className="animate-spin" />
                                        发布中...
                                    </>
                                ) : (
                                    <>
                                        <Sparkles size={18} />
                                        立即发布
                                    </>
                                )}
                            </Button>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default PublishPage;
