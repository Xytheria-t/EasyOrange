/**
 * @fileoverview 发布页面主入口
 * @description 商品发布页面，包含分类选择、图片上传、表单验证等功能
 * @version 2.0.0
 */

import '../../styles/main.css';
import '../../styles/floating-nav.css';
import '../../styles/publish.css';
import api from '../../api/index.js';
import { toast } from '../../utils/index.js';
import header from '../../components/Header.js';
import { navigation } from '../../app/navigation.js';
import { CategorySelector } from './CategorySelector.js';
import { ImageUploader } from './ImageUploader.js';
import { FormValidator } from './FormValidator.js';
import { DraftManager, type DraftData } from './DraftManager.js';
import { AUTO_SAVE_INTERVAL } from './constants.js';
import { isSuccessCode, type Result, type CreateProductRequest } from '../../types';

/** 发布页面元素接口 */
interface PublishPageElements {
    form: HTMLFormElement | null;
    productName: HTMLInputElement | null;
    nameCount: HTMLElement | null;
    productDescription: HTMLTextAreaElement | null;
    descCount: HTMLElement | null;
    salePrice: HTMLInputElement | null;
    originalPrice: HTMLInputElement | null;
    stockQuantity: HTMLInputElement | null;
    location: HTMLInputElement | null;
    contactMethod: HTMLInputElement | null;
    conditionOptions: HTMLElement | null;
    conditionLevel: HTMLInputElement | null;
    submitBtn: HTMLButtonElement | null;
    saveDraft: HTMLButtonElement | null;
    cancelBtn: HTMLButtonElement | null;
    progressFill: HTMLElement | null;
    discountDisplay: HTMLElement | null;
    discountValue: HTMLElement | null;
}

/**
 * 发布页面类
 * 负责商品发布的整体流程管理
 */
class PublishPage {
    private categorySelector: CategorySelector | null = null;
    private imageUploader: ImageUploader | null = null;
    private formValidator: FormValidator | null = null;
    private draftManager: DraftManager;
    private conditionLevel: number;
    private hasUnsavedChanges: boolean;
    private elements: PublishPageElements;

    constructor() {
        this.draftManager = new DraftManager();
        this.conditionLevel = 1;
        this.hasUnsavedChanges = false;
        this.elements = {
            form: null,
            productName: null,
            nameCount: null,
            productDescription: null,
            descCount: null,
            salePrice: null,
            originalPrice: null,
            stockQuantity: null,
            location: null,
            contactMethod: null,
            conditionOptions: null,
            conditionLevel: null,
            submitBtn: null,
            saveDraft: null,
            cancelBtn: null,
            progressFill: null,
            discountDisplay: null,
            discountValue: null
        };
        this.init();
    }

    /**
     * 初始化页面
     */
    private async init(): Promise<void> {
        this.cacheElements();
        this.bindEvents();
        if (!this.checkLoginStatus()) {return;}
        header.init();

        this.categorySelector = new CategorySelector(null, {
            onSelect: () => {
                this.hasUnsavedChanges = true;
                this.updateProgress();
            }
        });
        await this.categorySelector.loadCategories(api);

        this.imageUploader = new ImageUploader({
            onImagesChange: () => {
                this.hasUnsavedChanges = true;
                this.updateProgress();
            }
        });

        this.formValidator = new FormValidator();
        this.formValidator.initElements();

        this.loadDraft();
        this.initConditionOptions();
        this.initAutoSave();
        this.initBeforeUnload();
        this.initPasteImage();
        this.updateProgress();
    }

    /**
     * 缓存DOM元素引用
     */
    private cacheElements(): void {
        this.elements = {
            form: document.getElementById('publishForm') as HTMLFormElement | null,
            productName: document.getElementById('productName') as HTMLInputElement | null,
            nameCount: document.getElementById('nameCount'),
            productDescription: document.getElementById('productDescription') as HTMLTextAreaElement | null,
            descCount: document.getElementById('descCount'),
            salePrice: document.getElementById('salePrice') as HTMLInputElement | null,
            originalPrice: document.getElementById('originalPrice') as HTMLInputElement | null,
            stockQuantity: document.getElementById('stockQuantity') as HTMLInputElement | null,
            location: document.getElementById('location') as HTMLInputElement | null,
            contactMethod: document.getElementById('contactMethod') as HTMLInputElement | null,
            conditionOptions: document.getElementById('conditionOptions'),
            conditionLevel: document.getElementById('conditionLevel') as HTMLInputElement | null,
            submitBtn: document.getElementById('submitBtn') as HTMLButtonElement | null,
            saveDraft: document.getElementById('saveDraft') as HTMLButtonElement | null,
            cancelBtn: document.getElementById('cancelBtn') as HTMLButtonElement | null,
            progressFill: document.getElementById('progressFill'),
            discountDisplay: document.getElementById('discountDisplay'),
            discountValue: document.getElementById('discountValue')
        };
    }

    /**
     * 检查登录状态
     * @returns 是否已登录
     */
    private checkLoginStatus(): boolean {
        return navigation.requireAuth();
    }

    /**
     * 初始化成色选项
     */
    private initConditionOptions(): void {
        const options = this.elements.conditionOptions?.querySelectorAll('.condition-option');
        options?.forEach(option => {
            const htmlOption = option as HTMLElement;
            htmlOption.addEventListener('click', () => {
                options.forEach(opt => {
                    opt.classList.remove('selected');
                    opt.setAttribute('aria-checked', 'false');
                });
                htmlOption.classList.add('selected');
                htmlOption.setAttribute('aria-checked', 'true');
                const value = htmlOption.dataset.value;
                this.conditionLevel = value ? parseInt(value, 10) : 1;
                if (this.elements.conditionLevel) {
                    this.elements.conditionLevel.value = String(this.conditionLevel);
                }
                this.hasUnsavedChanges = true;
                this.updateProgress();
            });

            htmlOption.addEventListener('keydown', (e: KeyboardEvent) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    htmlOption.click();
                }
            });
        });
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        this.elements.form?.addEventListener('submit', (e: SubmitEvent) => this.handleSubmit(e));
        this.elements.saveDraft?.addEventListener('click', () => this.saveDraft());
        this.elements.cancelBtn?.addEventListener('click', () => this.handleCancel());

        this.elements.productName?.addEventListener('input', () => {
            this.updateCharCount('name');
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.productDescription?.addEventListener('input', () => {
            this.updateCharCount('desc');
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });

        this.elements.salePrice?.addEventListener('input', () => {
            this.formValidator?.validatePrice();
            this.calculateDiscount();
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.originalPrice?.addEventListener('input', () => {
            this.calculateDiscount();
            this.hasUnsavedChanges = true;
        });
        this.elements.stockQuantity?.addEventListener('input', () => {
            this.formValidator?.validateStock();
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });

        this.elements.productName?.addEventListener('blur', () => this.formValidator?.validateName());
        this.elements.productDescription?.addEventListener('blur', () => this.formValidator?.validateDescription());
        this.elements.location?.addEventListener('input', () => {
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.location?.addEventListener('blur', () => this.formValidator?.validateLocation());
        this.elements.contactMethod?.addEventListener('input', () => {
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.contactMethod?.addEventListener('blur', () => this.formValidator?.validateContact());
    }

    /**
     * 初始化自动保存
     */
    private initAutoSave(): void {
        setInterval(() => {
            if (this.hasUnsavedChanges) {
                this.saveDraft(true);
            }
        }, AUTO_SAVE_INTERVAL);
    }

    /**
     * 初始化页面离开提示
     */
    private initBeforeUnload(): void {
        window.addEventListener('beforeunload', (e: BeforeUnloadEvent) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = '您有未保存的更改，确定要离开吗？';
                return e.returnValue;
            }
        });
    }

    /**
     * 初始化粘贴图片功能
     */
    private initPasteImage(): void {
        document.addEventListener('paste', (e: ClipboardEvent) => {
            const items = e.clipboardData?.items;
            if (!items) {return;}

            for (const item of items) {
                if (item.type.startsWith('image/')) {
                    e.preventDefault();
                    const file = item.getAsFile();
                    if (file && this.imageUploader) {
                        this.imageUploader.processFiles([file] as unknown as FileList);
                    }
                    break;
                }
            }
        });
    }

    /**
     * 更新进度条
     */
    private updateProgress(): void {
        let completed = 0;
        const total = 5;

        if (this.categorySelector?.getSelected()) {completed++;}
        if ((this.elements.productName?.value.trim().length ?? 0) >= 2) {completed++;}
        if ((this.imageUploader?.getImages().length ?? 0) > 0) {completed++;}
        if (parseFloat(this.elements.salePrice?.value || '0') > 0 && parseInt(this.elements.stockQuantity?.value || '0', 10) > 0) {completed++;}
        if ((this.elements.location?.value.trim() ?? '') && (this.elements.contactMethod?.value.trim() ?? '')) {completed++;}

        const percentage = (completed / total) * 100;
        if (this.elements.progressFill) {
            this.elements.progressFill.style.width = `${percentage}%`;
        }

        const steps = document.querySelectorAll('.progress-step');
        steps.forEach((step, index) => {
            step.classList.remove('active', 'completed');
            if (index < completed) {
                step.classList.add('completed');
            } else if (index === completed) {
                step.classList.add('active');
            }
        });
    }

    /**
     * 计算折扣
     */
    private calculateDiscount(): void {
        const salePrice = parseFloat(this.elements.salePrice?.value || '0');
        const originalPrice = parseFloat(this.elements.originalPrice?.value || '0');

        if (salePrice > 0 && originalPrice > salePrice) {
            const discount = (salePrice / originalPrice * 10).toFixed(1);
            if (this.elements.discountValue) {
                this.elements.discountValue.textContent = discount;
            }
            if (this.elements.discountDisplay) {
                this.elements.discountDisplay.style.display = 'flex';
            }
        } else {
            if (this.elements.discountDisplay) {
                this.elements.discountDisplay.style.display = 'none';
            }
        }
    }

    /**
     * 更新字符计数
     * @param type - 类型：name 或 desc
     */
    private updateCharCount(type: 'name' | 'desc'): void {
        if (type === 'name') {
            const count = this.elements.productName?.value.length || 0;
            if (this.elements.nameCount) {
                this.elements.nameCount.textContent = String(count);
            }
        } else if (type === 'desc') {
            const count = this.elements.productDescription?.value.length || 0;
            if (this.elements.descCount) {
                this.elements.descCount.textContent = String(count);
            }
        }
    }

    /**
     * 处理表单提交
     * @param e - 提交事件
     */
    private async handleSubmit(e: SubmitEvent): Promise<void> {
        e.preventDefault();

        if (!this.checkLoginStatus()) {return;}

        const categoryValid = this.categorySelector?.validate();
        const imagesValid = this.imageUploader?.validate();
        const formValid = this.formValidator?.validateAll();

        if (!categoryValid || !imagesValid || !formValid) {
            toast.error('请检查表单填写是否正确');
            return;
        }

        if (this.elements.submitBtn) {
            this.elements.submitBtn.disabled = true;
            this.elements.submitBtn.innerHTML = `
                <svg class="animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="animation: spin 1s linear infinite;">
                    <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/>
                    <path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"/>
                </svg>
                上传图片中...
            `;
        }

        try {
            toast.info('正在上传图片...');
            const imageUrls = await this.imageUploader?.uploadAll();

            if (!imageUrls || imageUrls.length === 0) {
                throw new Error('图片上传失败');
            }

            if (this.elements.submitBtn) {
                this.elements.submitBtn.innerHTML = `
                    <svg class="animate-spin" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="animation: spin 1s linear infinite;">
                        <circle cx="12" cy="12" r="10" stroke-opacity="0.25"/>
                        <path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"/>
                    </svg>
                    发布中...
                `;
            }

            const selectedCategory = this.categorySelector?.getSelected();
            const productData: CreateProductRequest = {
                categoryId: selectedCategory?.id ?? 0,
                title: this.elements.productName?.value.trim() || '',
                description: this.elements.productDescription?.value.trim() || '',
                price: parseFloat(this.elements.salePrice?.value || '0'),
                originalPrice: parseFloat(this.elements.originalPrice?.value || '0') || undefined,
                condition: this.conditionLevel === 1 ? 'NEW' : this.conditionLevel === 2 ? 'LIKE_NEW' : this.conditionLevel === 3 ? 'GOOD' : 'FAIR',
                location: this.elements.location?.value.trim() || '',
                images: imageUrls
            };

            const response = await api.product.createProduct(productData);

            if (isSuccessCode(response.code)) {
                toast.success('商品发布成功！');
                this.draftManager.clear();
                this.hasUnsavedChanges = false;

                setTimeout(() => {
                    navigation.replace('products');
                }, 1500);
            } else {
                throw new Error((response as Result).message || '发布失败');
            }
        } catch (error) {
            const errorMessage = error instanceof Error ? error.message : '发布失败，请稍后重试';
            toast.error(errorMessage);
        } finally {
            if (this.elements.submitBtn) {
                this.elements.submitBtn.disabled = false;
                this.elements.submitBtn.innerHTML = `
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M22 2L11 13"/>
                        <path d="M22 2L15 22L11 13L2 9L22 2Z"/>
                    </svg>
                    立即发布
                `;
            }
        }
    }

    /**
     * 保存草稿
     * @param silent - 是否静默保存
     */
    saveDraft(silent = false): void {
        const draft: DraftData = {
            categoryId: this.categorySelector?.getSelected()?.id || null,
            name: this.elements.productName?.value,
            description: this.elements.productDescription?.value,
            price: this.elements.salePrice?.value,
            originalPrice: this.elements.originalPrice?.value,
            stock: this.elements.stockQuantity?.value,
            conditionLevel: this.conditionLevel,
            location: this.elements.location?.value,
            contactMethod: this.elements.contactMethod?.value
        };

        this.draftManager.save(draft);

        if (!silent) {
            toast.success('草稿已保存');
        }

        this.hasUnsavedChanges = false;
    }

    /**
     * 加载草稿
     */
    private loadDraft(): void {
        const draft = this.draftManager.load();
        if (!draft) {return;}

        if (draft.name && this.elements.productName) {
            this.elements.productName.value = draft.name;
            this.updateCharCount('name');
        }

        if (draft.description && this.elements.productDescription) {
            this.elements.productDescription.value = draft.description;
            this.updateCharCount('desc');
        }

        if (draft.price && this.elements.salePrice) {
            this.elements.salePrice.value = draft.price;
        }

        if (draft.originalPrice && this.elements.originalPrice) {
            this.elements.originalPrice.value = draft.originalPrice;
        }

        if (draft.stock && this.elements.stockQuantity) {
            this.elements.stockQuantity.value = draft.stock;
        }

        if (draft.conditionLevel) {
            this.conditionLevel = draft.conditionLevel;
            if (this.elements.conditionLevel) {
                this.elements.conditionLevel.value = String(draft.conditionLevel);
            }
            const options = this.elements.conditionOptions?.querySelectorAll('.condition-option');
            options?.forEach(opt => {
                const htmlOpt = opt as HTMLElement;
                const isSelected = parseInt(htmlOpt.dataset.value || '0', 10) === draft.conditionLevel;
                htmlOpt.classList.toggle('selected', isSelected);
                htmlOpt.setAttribute('aria-checked', isSelected.toString());
            });
        }

        if (draft.location && this.elements.location) {
            this.elements.location.value = draft.location;
        }

        if (draft.contactMethod && this.elements.contactMethod) {
            this.elements.contactMethod.value = draft.contactMethod;
        }

        this.calculateDiscount();
        this.updateProgress();
    }

    /**
     * 处理取消操作
     */
    private handleCancel(): void {
        if (this.hasUnsavedChanges) {
            if (confirm('您有未保存的更改，确定要取消吗？')) {
                this.hasUnsavedChanges = false;
                navigation.replace('home');
            }
        } else {
            navigation.replace('home');
        }
    }
}

document.addEventListener('DOMContentLoaded', () => {
    new PublishPage();
});
