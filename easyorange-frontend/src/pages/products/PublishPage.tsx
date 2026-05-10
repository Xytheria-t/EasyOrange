import { useState, useRef, useCallback, useEffect } from 'react';
import { useNavigate } from 'react-router-dom';
import {
  Camera, Loader2, Sparkles, ImageIcon, Tag, FileText,
  DollarSign, MapPin, MessageCircle, Package, ChevronRight,
  Check, AlertCircle, Upload, Trash2, GripVertical, Info
} from 'lucide-react';
import { useCreateProduct, useCategories } from '@/hooks';
import { uploadFile } from '@/api/uploadApi';
import { productApi } from '@/api/productApi';
import { compressImage } from '@/utils/imageCompress';
import { CONDITION_LABEL_MAP } from '@/constants';
import { useUIStore } from '@/store/uiStore';
import './publish.css';

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
  const addToast = useUIStore((s) => s.addToast);
  const [form, setForm] = useState<FormState>(INITIAL_FORM);
  const [errors, setErrors] = useState<FormErrors>({});
  const [uploadingIndex, setUploadingIndex] = useState<number | null>(null);
  const [dragOverIndex, setDragOverIndex] = useState<number | null>(null);
  const [isDragging, setIsDragging] = useState(false);
  const [activeSection, setActiveSection] = useState(0);
  const fileInputRef = useRef<HTMLInputElement>(null);
  const dragItemRef = useRef<number | null>(null);
  const pageRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const timer = setTimeout(() => {
      if (pageRef.current) {
        pageRef.current.classList.add('page-ready');
      }
    }, 100);
    return () => clearTimeout(timer);
  }, []);

  const validate = (): boolean => {
    const newErrors: FormErrors = {};
    if (!form.name.trim()) {
      newErrors.name = '请输入商品名称';
    }
    if (!form.price || Number(form.price) <= 0 || isNaN(Number(form.price))) {
      newErrors.price = '请输入有效价格';
    }
    if (!form.categoryId || Number(form.categoryId) <= 0) {
      newErrors.categoryId = '请选择商品类别';
    }
    if (!form.conditionLevel || Number(form.conditionLevel) <= 0) {
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
    if (!files) {return;}
    await processFiles(Array.from(files));
    if (fileInputRef.current) {
      fileInputRef.current.value = '';
    }
  };

  const processFiles = async (files: File[]) => {
    for (const file of files) {
      if (form.imageUrls.length >= 9) {break;}
      if (!file.type.startsWith('image/')) {continue;}
      if (file.size > 10 * 1024 * 1024) {continue;}

      const index = form.imageUrls.length;
      setUploadingIndex(index);
      try {
        const compressed = await compressImage(file);
        const result = await uploadFile(compressed);
        if (result.data?.url) {
          setForm(prev => ({ ...prev, imageUrls: [...prev.imageUrls, result.data.url] }));
        }
      } catch (error) {
        console.error('Image upload failed:', error);
        addToast({ type: 'error', message: '图片上传失败，请重试' });
      } finally {
        setUploadingIndex(null);
      }
    }
  };

  const handleImageUrlRemove = (index: number) => {
    setForm(prev => ({ ...prev, imageUrls: prev.imageUrls.filter((_, i) => i !== index) }));
  };

  const handleDragStart = (index: number) => {
    dragItemRef.current = index;
  };

  const handleDragOver = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    setDragOverIndex(index);
  }, []);

  const handleDrop = useCallback((e: React.DragEvent, index: number) => {
    e.preventDefault();
    setDragOverIndex(null);
    const draggedIndex = dragItemRef.current;
    if (draggedIndex === null || draggedIndex === index) {return;}

    setForm(prev => {
      const newUrls = [...prev.imageUrls];
      const [removed] = newUrls.splice(draggedIndex, 1);
      newUrls.splice(index, 0, removed);
      return { ...prev, imageUrls: newUrls };
    });
    dragItemRef.current = null;
  }, []);

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

  const handleSubmit = async (isDraft: boolean) => {
    if (!validate()) {return;}

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
      const productId = await createProduct.mutateAsync(payload) as string;

      if (!isDraft && productId) {
        await productApi.putOnline(productId);
      }

      navigate(`/products/${productId}`);
    } catch {
      // error handled by mutation state
    }
  };

  const isSubmitting = createProduct.isPending;
  const progress = Math.min(100, Math.round(
    ((form.imageUrls.length > 0 ? 1 : 0) +
      (form.name ? 1 : 0) +
      (form.price ? 1 : 0) +
      (form.categoryId ? 1 : 0) +
      (form.conditionLevel ? 1 : 0)) / 5 * 100
  ));

  const sections = [
    { id: 'images', label: '商品图片', icon: ImageIcon },
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
              <span>发布您的宝贝</span>
            </div>
            <h1 className="page-title-v2">
              <span className="gradient-text">发布商品</span>
            </h1>
            <p className="page-subtitle-v2">
              精心填写信息，让您的闲置物品找到新主人
            </p>
          </div>

          {/* Progress Bar */}
          <div className="progress-section-v2">
            <div className="progress-header">
              <span className="progress-label">完成度</span>
              <span className="progress-value">{progress}%</span>
            </div>
            <div className="progress-bar-v2">
              <div
                className="progress-fill-v2"
                style={{ width: `${progress}%` }}
              />
            </div>
            <div className="progress-steps-v2">
              {sections.map((section, index) => {
                const Icon = section.icon;
                const isActive = index <= activeSection;
                const isCompleted = index < activeSection ||
                  (index === 0 && form.imageUrls.length > 0) ||
                  (index === 1 && form.name && form.categoryId && form.conditionLevel) ||
                  (index === 2 && form.description) ||
                  (index === 3 && form.price);
                return (
                  <button
                    key={section.id}
                    className={`progress-step-v2 ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`}
                    onClick={() => {
                      setActiveSection(index);
                      document.getElementById(section.id)?.scrollIntoView({ behavior: 'smooth', block: 'start' });
                    }}
                  >
                    <div className="step-icon-v2">
                      {isCompleted ? <Check size={14} /> : <Icon size={14} />}
                    </div>
                    <span className="step-label-v2">{section.label}</span>
                  </button>
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
                  <h2 className="section-title-v2">商品图片</h2>
                  <span className="section-hint-v2">最多9张，首张为封面</span>
                </div>
                <span className="required-badge">必填</span>
              </div>

              <div
                className={`upload-zone-v2 ${isDragging ? 'dragover' : ''} ${errors.imageUrls ? 'has-error' : ''}`}
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
                {form.imageUrls.length === 0 ? (
                  <button
                    type="button"
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
                      <span className="upload-desc">支持 JPG、PNG、WEBP 格式，单张不超过 10MB</span>
                    </div>
                  </button>
                ) : (
                  <div className="image-grid-v2">
                    {form.imageUrls.map((url, index) => (
                      <div
                        key={`${url}-${index}`}
                        className={`image-item-v2 ${dragOverIndex === index ? 'drag-over' : ''} ${index === 0 ? 'is-cover' : ''}`}
                        draggable
                        onDragStart={() => handleDragStart(index)}
                        onDragOver={(e) => handleDragOver(e, index)}
                        onDrop={(e) => handleDrop(e, index)}
                        onDragEnd={handleDragEnd}
                      >
                        <img src={url} alt={`商品图片 ${index + 1}`} width="120" height="120" />
                        {index === 0 && (
                          <div className="cover-badge-v2">
                            <span>封面</span>
                          </div>
                        )}
                        <div className="image-actions-v2">
                          <button
                            type="button"
                            className="image-action-btn"
                            onClick={() => handleImageUrlRemove(index)}
                            title="删除"
                          >
                            <Trash2 size={14} />
                          </button>
                        </div>
                        <div className="drag-handle-v2">
                          <GripVertical size={14} />
                        </div>
                      </div>
                    ))}
                    {form.imageUrls.length < 9 && (
                      <button
                        type="button"
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
                      </button>
                    )}
                  </div>
                )}
              </div>
              {errors.imageUrls && (
                <div className="error-message-v2">
                  <AlertCircle size={14} />
                  <span>{errors.imageUrls}</span>
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
                  <span className="section-hint-v2">填写商品的核心信息</span>
                </div>
              </div>

              <div className="form-fields-v2">
                <div className="field-group-v2">
                  <label className="field-label-v2">
                    商品名称
                    <span className="required-mark">*</span>
                  </label>
                  <div className={`input-wrapper-v2 ${errors.name ? 'has-error' : ''}`}>
                    <input
                      type="text"
                      name="name"
                      placeholder="给宝贝起个吸引人的名字"
                      value={form.name}
                      onChange={e => updateField('name', e.target.value)}
                      maxLength={200}
                      className="field-input-v2"
                    />
                    <span className="char-count-v2">{form.name.length}/200</span>
                  </div>
                  {errors.name && (
                    <div className="error-message-v2">
                      <AlertCircle size={14} />
                      <span>{errors.name}</span>
                    </div>
                  )}
                </div>

                <div className="field-row-v2">
                  <div className="field-group-v2">
                    <label className="field-label-v2">
                      商品类别
                      <span className="required-mark">*</span>
                    </label>
                    <div className={`select-wrapper-v2 ${errors.categoryId ? 'has-error' : ''}`}>
                      <select
                        value={form.categoryId}
                        onChange={e => updateField('categoryId', e.target.value)}
                        className="field-select-v2"
                      >
                        <option value="">选择类别</option>
                        {categories?.map(cat => (
                          <option key={cat.id} value={cat.id}>{cat.name}</option>
                        ))}
                      </select>
                      <ChevronRight size={16} className="select-arrow" />
                    </div>
                    {errors.categoryId && (
                      <div className="error-message-v2">
                        <AlertCircle size={14} />
                        <span>{errors.categoryId}</span>
                      </div>
                    )}
                  </div>

                  <div className="field-group-v2">
                    <label className="field-label-v2">
                      新旧程度
                      <span className="required-mark">*</span>
                    </label>
                    <div className={`select-wrapper-v2 ${errors.conditionLevel ? 'has-error' : ''}`}>
                      <select
                        name="conditionLevel"
                        value={form.conditionLevel}
                        onChange={e => updateField('conditionLevel', e.target.value)}
                        className="field-select-v2"
                      >
                        <option value="">选择成色</option>
                        {Object.entries(CONDITION_LABEL_MAP).map(([code, label]) => (
                          <option key={code} value={code}>{label}</option>
                        ))}
                      </select>
                      <ChevronRight size={16} className="select-arrow" />
                    </div>
                    {errors.conditionLevel && (
                      <div className="error-message-v2">
                        <AlertCircle size={14} />
                        <span>{errors.conditionLevel}</span>
                      </div>
                    )}
                  </div>
                </div>

                {form.conditionLevel && (
                  <div className="condition-preview">
                    <div className="condition-icon-large">
                      {CONDITION_ICONS[Number(form.conditionLevel)]}
                    </div>
                    <div className="condition-info">
                      <span className="condition-name">
                        {CONDITION_LABEL_MAP[Number(form.conditionLevel)]}
                      </span>
                      <span className="condition-desc">
                        {CONDITION_DESC[Number(form.conditionLevel)]}
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
                  <span className="section-hint-v2">详细描述能提高成交率</span>
                </div>
              </div>

              <div className="form-fields-v2">
                <div className="field-group-v2">
                  <label className="field-label-v2">商品描述</label>
                  <div className="textarea-wrapper-v2">
                    <textarea
                      rows={5}
                      placeholder="详细描述商品的品牌、型号、规格、使用情况等信息，让买家更了解您的宝贝..."
                      value={form.description}
                      onChange={e => updateField('description', e.target.value)}
                      maxLength={2000}
                      className="field-textarea-v2"
                    />
                    <span className="char-count-v2">{form.description.length}/2000</span>
                  </div>
                </div>

                <div className="field-row-v2">
                  <div className="field-group-v2">
                    <label className="field-label-v2">
                      <MapPin size={14} />
                      交易地点
                    </label>
                    <div className="input-wrapper-v2">
                      <input
                        type="text"
                        name="location"
                        placeholder="如：清水河校区南门"
                        value={form.location}
                        onChange={e => updateField('location', e.target.value)}
                        maxLength={100}
                        className="field-input-v2"
                      />
                    </div>
                  </div>

                  <div className="field-group-v2">
                    <label className="field-label-v2">
                      <MessageCircle size={14} />
                      联系方式
                    </label>
                    <div className="input-wrapper-v2">
                      <input
                        type="text"
                        placeholder="微信号 / QQ号"
                        value={form.contactMethod}
                        onChange={e => updateField('contactMethod', e.target.value)}
                        maxLength={50}
                        className="field-input-v2"
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
                  <span className="section-hint-v2">合理定价更容易成交</span>
                </div>
              </div>

              <div className="form-fields-v2">
                <div className="field-row-v2">
                  <div className="field-group-v2">
                    <label className="field-label-v2">
                      出售价格
                      <span className="required-mark">*</span>
                    </label>
                    <div className={`input-wrapper-v2 price-input-wrapper ${errors.price ? 'has-error' : ''}`}>
                      <span className="price-symbol-v2">¥</span>
                      <input
                        type="number"
                        name="price"
                        placeholder="0.00"
                        step="0.01"
                        min="0.01"
                        value={form.price}
                        onChange={e => updateField('price', e.target.value)}
                        className="field-input-v2 price-input"
                      />
                    </div>
                    {errors.price && (
                      <div className="error-message-v2">
                        <AlertCircle size={14} />
                        <span>{errors.price}</span>
                      </div>
                    )}
                  </div>

                  <div className="field-group-v2">
                    <label className="field-label-v2">原价（选填）</label>
                    <div className="input-wrapper-v2 price-input-wrapper">
                      <span className="price-symbol-v2">¥</span>
                      <input
                        type="number"
                        placeholder="0.00"
                        step="0.01"
                        min="0.01"
                        value={form.originalPrice}
                        onChange={e => updateField('originalPrice', e.target.value)}
                        className="field-input-v2 price-input"
                      />
                    </div>
                  </div>
                </div>

                {form.price && form.originalPrice && Number(form.originalPrice) > Number(form.price) && (
                  <div className="discount-tag">
                    <span className="discount-badge">
                      省 {Math.round((1 - Number(form.price) / Number(form.originalPrice)) * 100)}%
                    </span>
                    <span className="discount-text">
                      比原价优惠 ¥{(Number(form.originalPrice) - Number(form.price)).toFixed(2)}
                    </span>
                  </div>
                )}

                <div className="field-group-v2" style={{ maxWidth: '200px' }}>
                  <label className="field-label-v2">
                    <Package size={14} />
                    库存数量
                  </label>
                  <div className="input-wrapper-v2">
                    <input
                      type="number"
                      name="stock"
                      placeholder="1"
                      min="1"
                      value={form.stock}
                      onChange={e => updateField('stock', e.target.value)}
                      className="field-input-v2"
                    />
                    <span className="stock-unit">件</span>
                  </div>
                </div>
              </div>
            </section>

            {/* Info Banner */}
            <div className="info-banner-v2">
              <Info size={18} />
              <p>发布前请确保商品信息真实有效，定价合理。禁止发布违规商品。</p>
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
              <button
                className="btn-draft-v2"
                onClick={() => handleSubmit(true)}
                disabled={isSubmitting}
              >
                保存草稿
              </button>
              <button
                className="btn-publish-v2"
                onClick={() => handleSubmit(false)}
                disabled={isSubmitting}
              >
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
              </button>
            </div>
          </div>
        </div>
      </div>
    </div>
  );
}

export default PublishPage;
