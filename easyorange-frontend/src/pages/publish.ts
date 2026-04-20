import '../styles/main.css';
import '../styles/floating-nav.css';
import '../styles/publish.css';
import api from '../api/index.js';
import { toast, storage } from '../utils/index.js';
import header from '../components/Header.js';
import { navigation } from '../app/navigation.js';
import { isSuccessCode, type ApiCode, type Category, type CreateProductRequest } from '../types';

// ============================================
// 常量定义
// ============================================

const MAX_IMAGES = 9;
const MAX_FILE_SIZE = 5 * 1024 * 1024;

// ============================================
// 类型定义
// ============================================

/** 图片数据接口 */
interface ImageData {
    id: number;
    file: File;
    url: string;
    uploaded: boolean;
    serverUrl: string | null;
}

/** 发布页面 DOM 元素缓存 */
interface PublishPageElements {
    form: HTMLFormElement | null;
    categoryCascade: HTMLElement | null;
    level1: HTMLElement | null;
    level2: HTMLElement | null;
    level3: HTMLElement | null;
    level1Options: HTMLElement | null;
    level2Options: HTMLElement | null;
    level3Options: HTMLElement | null;
    selectedCategoryId: HTMLInputElement | null;
    categoryError: HTMLElement | null;
    productName: HTMLInputElement | null;
    nameCount: HTMLElement | null;
    nameError: HTMLElement | null;
    productDescription: HTMLTextAreaElement | null;
    descCount: HTMLElement | null;
    descError: HTMLElement | null;
    uploadArea: HTMLElement | null;
    fileInput: HTMLInputElement | null;
    previewContainer: HTMLElement | null;
    previewList: HTMLElement | null;
    imageError: HTMLElement | null;
    salePrice: HTMLInputElement | null;
    priceError: HTMLElement | null;
    originalPrice: HTMLInputElement | null;
    stockQuantity: HTMLInputElement | null;
    stockError: HTMLElement | null;
    conditionOptions: HTMLElement | null;
    conditionLevel: HTMLInputElement | null;
    conditionError: HTMLElement | null;
    location: HTMLInputElement | null;
    locationError: HTMLElement | null;
    contactMethod: HTMLInputElement | null;
    contactError: HTMLElement | null;
    submitBtn: HTMLButtonElement | null;
    saveDraft: HTMLButtonElement | null;
    cancelBtn: HTMLButtonElement | null;
    toastContainer: HTMLElement | null;
    progressFill: HTMLElement | null;
    discountDisplay: HTMLElement | null;
    discountValue: HTMLElement | null;
    userMenu: HTMLElement | null;
    userAvatarBtn: HTMLElement | null;
    userAvatarImg: HTMLImageElement | null;
    userName: HTMLElement | null;
    logoutBtn: HTMLElement | null;
    loginBtn: HTMLElement | null;
    notificationBtn: HTMLElement | null;
}

/** 草稿数据接口 */
interface DraftData {
    categoryId: number | null;
    name: string;
    description: string;
    price: string;
    originalPrice: string;
    stock: string;
    conditionLevel: number;
    location: string;
    contactMethod: string;
    savedAt: string;
}

/** 商品创建请求数据 */
interface ProductCreateData {
    categoryId: number;
    title: string;
    name: string;
    description: string;
    price: number;
    originalPrice: number | null;
    stock: number;
    conditionLevel: number;
    condition: 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'POOR';
    location: string;
    contactMethod: string;
    images: string[];
    status: number;
}

/** 图片上传响应 */
interface UploadResponse {
    code: ApiCode;
    message: string;
    data: {
        url: string;
        filename: string;
        size: number;
        type: string;
    };
}

// ============================================
// PublishPage 类
// ============================================

class PublishPage {
    private categories: Category[];
    private selectedCategory: Category | null;
    private images: ImageData[];
    private maxImages: number;
    private maxFileSize: number;
    private conditionLevel: number;
    private hasUnsavedChanges: boolean;
    private draggedItem: number | null;
    private autoSaveInterval: ReturnType<typeof setInterval> | null;
    private elements: PublishPageElements;

    constructor() {
        this.categories = [];
        this.selectedCategory = null;
        this.images = [];
        this.maxImages = MAX_IMAGES;
        this.maxFileSize = MAX_FILE_SIZE;
        this.conditionLevel = 1;
        this.hasUnsavedChanges = false;
        this.draggedItem = null;
        this.autoSaveInterval = null;
        this.elements = {} as PublishPageElements;
        this.init();
    }

    private async init(): Promise<void> {
        this.cacheElements();
        this.bindEvents();
        if (!this.checkLoginStatus()) {return;}
        header.init();
        header.setActiveNav('publish');
        await this.loadCategories();
        this.loadDraft();
        this.initConditionOptions();
        this.initAutoSave();
        this.initBeforeUnload();
        this.initPasteImage();
        this.updateProgress();
    }

    private cacheElements(): void {
        this.elements = {
            form: document.getElementById('publishForm') as HTMLFormElement | null,
            categoryCascade: document.getElementById('categoryCascade'),
            level1: document.getElementById('level1'),
            level2: document.getElementById('level2'),
            level3: document.getElementById('level3'),
            level1Options: document.getElementById('level1Options'),
            level2Options: document.getElementById('level2Options'),
            level3Options: document.getElementById('level3Options'),
            selectedCategoryId: document.getElementById('selectedCategoryId') as HTMLInputElement | null,
            categoryError: document.getElementById('categoryError'),
            productName: document.getElementById('productName') as HTMLInputElement | null,
            nameCount: document.getElementById('nameCount'),
            nameError: document.getElementById('nameError'),
            productDescription: document.getElementById('productDescription') as HTMLTextAreaElement | null,
            descCount: document.getElementById('descCount'),
            descError: document.getElementById('descError'),
            uploadArea: document.getElementById('uploadArea'),
            fileInput: document.getElementById('fileInput') as HTMLInputElement | null,
            previewContainer: document.getElementById('previewContainer'),
            previewList: document.getElementById('previewList'),
            imageError: document.getElementById('imageError'),
            salePrice: document.getElementById('salePrice') as HTMLInputElement | null,
            priceError: document.getElementById('priceError'),
            originalPrice: document.getElementById('originalPrice') as HTMLInputElement | null,
            stockQuantity: document.getElementById('stockQuantity') as HTMLInputElement | null,
            stockError: document.getElementById('stockError'),
            conditionOptions: document.getElementById('conditionOptions'),
            conditionLevel: document.getElementById('conditionLevel') as HTMLInputElement | null,
            conditionError: document.getElementById('conditionError'),
            location: document.getElementById('location') as HTMLInputElement | null,
            locationError: document.getElementById('locationError'),
            contactMethod: document.getElementById('contactMethod') as HTMLInputElement | null,
            contactError: document.getElementById('contactError'),
            submitBtn: document.getElementById('submitBtn') as HTMLButtonElement | null,
            saveDraft: document.getElementById('saveDraft') as HTMLButtonElement | null,
            cancelBtn: document.getElementById('cancelBtn') as HTMLButtonElement | null,
            toastContainer: document.getElementById('toastContainer'),
            progressFill: document.getElementById('progressFill'),
            discountDisplay: document.getElementById('discountDisplay'),
            discountValue: document.getElementById('discountValue'),
            userMenu: document.getElementById('userMenu'),
            userAvatarBtn: document.getElementById('userAvatarBtn'),
            userAvatarImg: document.getElementById('userAvatarImg') as HTMLImageElement | null,
            userName: document.getElementById('userName'),
            logoutBtn: document.getElementById('logoutBtn'),
            loginBtn: document.getElementById('loginBtn'),
            notificationBtn: document.getElementById('notificationBtn')
        };
    }

    private checkLoginStatus(): boolean {
        return navigation.requireAuth();
    }

    private initConditionOptions(): void {
        const options = this.elements.conditionOptions?.querySelectorAll('.condition-option');
        if (!options) {return;}

        options.forEach(option => {
            option.addEventListener('click', () => {
                options.forEach(opt => {
                    opt.classList.remove('selected');
                    opt.setAttribute('aria-checked', 'false');
                });
                option.classList.add('selected');
                option.setAttribute('aria-checked', 'true');
                const value = option.getAttribute('data-value');
                this.conditionLevel = value ? parseInt(value) : 1;
                if (this.elements.conditionLevel) {
                    this.elements.conditionLevel.value = String(this.conditionLevel);
                }
                this.clearError('condition');
                this.hasUnsavedChanges = true;
                this.updateProgress();
            });
            
            option.addEventListener('keydown', ((e: KeyboardEvent) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    (option as HTMLElement).click();
                }
            }) as EventListener);
        });
        
        options[0]?.classList.add('selected');
    }

    private bindEvents(): void {
        this.elements.form?.addEventListener('submit', (e) => this.handleSubmit(e));
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
        
        this.elements.uploadArea?.addEventListener('click', () => this.elements.fileInput?.click());
        this.elements.uploadArea?.addEventListener('keydown', (e: KeyboardEvent) => {
            if (e.key === 'Enter' || e.key === ' ') {
                e.preventDefault();
                this.elements.fileInput?.click();
            }
        });
        this.elements.fileInput?.addEventListener('change', (e) => this.handleFileSelect(e));
        
        this.elements.uploadArea?.addEventListener('dragover', (e) => this.handleDragOver(e));
        this.elements.uploadArea?.addEventListener('dragleave', (e) => this.handleDragLeave(e));
        this.elements.uploadArea?.addEventListener('drop', (e) => this.handleDrop(e));
        
        this.elements.salePrice?.addEventListener('input', () => {
            this.validatePrice();
            this.calculateDiscount();
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.originalPrice?.addEventListener('input', () => {
            this.calculateDiscount();
            this.hasUnsavedChanges = true;
        });
        this.elements.stockQuantity?.addEventListener('input', () => {
            this.validateStock();
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        
        this.elements.productName?.addEventListener('blur', () => this.validateName());
        this.elements.productDescription?.addEventListener('blur', () => this.validateDescription());
        this.elements.location?.addEventListener('input', () => {
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.location?.addEventListener('blur', () => this.validateLocation());
        this.elements.contactMethod?.addEventListener('input', () => {
            this.hasUnsavedChanges = true;
            this.updateProgress();
        });
        this.elements.contactMethod?.addEventListener('blur', () => this.validateContact());
    }
    
    private initAutoSave(): void {
        this.autoSaveInterval = setInterval(() => {
            if (this.hasUnsavedChanges) {
                this.saveDraft(true);
            }
        }, 30000);
    }

    private initBeforeUnload(): void {
        window.addEventListener('beforeunload', (e: BeforeUnloadEvent) => {
            if (this.hasUnsavedChanges) {
                e.preventDefault();
                e.returnValue = '您有未保存的更改，确定要离开吗？';
                return e.returnValue;
            }
        });
    }

    private initPasteImage(): void {
        document.addEventListener('paste', (e: ClipboardEvent) => {
            const items = e.clipboardData?.items;
            if (!items) {return;}
            
            for (const item of items) {
                if (item.type.startsWith('image/')) {
                    e.preventDefault();
                    const file = item.getAsFile();
                    if (file) {
                        this.processFiles([file]);
                    }
                    break;
                }
            }
        });
    }

    private updateProgress(): void {
        let completed = 0;
        const total = 5;
        
        if (this.selectedCategory) {completed++;}
        if (this.elements.productName && this.elements.productName.value.trim().length >= 2) {completed++;}
        if (this.images.length > 0) {completed++;}
        if (this.elements.salePrice && this.elements.stockQuantity && 
            parseFloat(this.elements.salePrice.value) > 0 && 
            parseInt(this.elements.stockQuantity.value) > 0) {completed++;}
        if (this.elements.location && this.elements.contactMethod && 
            this.elements.location.value.trim() && this.elements.contactMethod.value.trim()) {completed++;}
        
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

    private async loadCategories(retryCount = 0): Promise<void> {
        const maxRetries = 3;
        const retryDelay = 1000;
        
        try {
            this.showCategoryLoading(true);
            
            const response = await api.product.getCategoryTree();
            
            if (response) {
                const data = (response as { data?: Category[] }).data ?? (response as unknown as Category[]);
                this.categories = Array.isArray(data) ? data : [];
                this.renderCategoryLevel(1, this.categories);
                this.showCategoryLoading(false);
            } else {
                throw new Error('分类数据格式错误');
            }
        } catch {
            
            if (retryCount < maxRetries) {
                toast.warning(`加载分类失败，正在重试...`);
                
                await new Promise(resolve => setTimeout(resolve, retryDelay));
                return this.loadCategories(retryCount + 1);
            }
            
            this.showCategoryLoading(false);
            this.showCategoryError('加载分类失败，请检查网络连接后刷新页面');
            toast.error('加载分类失败，请刷新页面重试');
        }
    }

    private showCategoryLoading(show: boolean): void {
        const level1Options = this.elements.level1Options;
        if (!level1Options) {return;}
        
        if (show) {
            level1Options.innerHTML = `
                <div class="cascade-loading">
                    <div class="loading-spinner"></div>
                    <span>正在加载分类...</span>
                </div>
            `;
        }
    }

    private showCategoryError(message: string): void {
        const level1Options = this.elements.level1Options;
        if (!level1Options) {return;}
        
        level1Options.innerHTML = `
            <div class="cascade-error">
                <span class="error-icon">⚠️</span>
                <span>${message}</span>
                <button class="retry-btn" onclick="location.reload()">刷新页面</button>
            </div>
        `;
    }

    private renderCategoryLevel(level: number, categories: Category[]): void {
        const optionsContainer = this.elements[`level${level}Options` as keyof PublishPageElements] as HTMLElement | null;
        if (!optionsContainer) {return;}
        
        optionsContainer.innerHTML = '';
        
        categories.forEach(category => {
            const option = document.createElement('div');
            option.className = 'cascade-option';
            option.dataset.id = String(category.id);
            option.dataset.level = String(level);
            
            const icon = category.icon || '📁';
            option.innerHTML = `
                <span class="cascade-icon">${icon}</span>
                <span class="cascade-name">${category.name}</span>
            `;
            
            option.addEventListener('click', () => this.selectCategory(level, category));
            
            optionsContainer.appendChild(option);
        });
        
        const levelElement = this.elements[`level${level}` as keyof PublishPageElements] as HTMLElement | null;
        if (levelElement) {
            levelElement.classList.remove('hidden');
        }
    }

    private selectCategory(level: number, category: Category): void {
        const optionsContainer = this.elements[`level${level}Options` as keyof PublishPageElements] as HTMLElement | null;
        const options = optionsContainer?.querySelectorAll('.cascade-option');
        
        options?.forEach(opt => opt.classList.remove('selected'));
        const selectedOption = optionsContainer?.querySelector(`[data-id="${category.id}"]`);
        if (selectedOption) {
            selectedOption.classList.add('selected');
        }
        
        for (let i = level + 1; i <= 3; i++) {
            const levelElement = this.elements[`level${i}` as keyof PublishPageElements] as HTMLElement | null;
            if (levelElement) {
                levelElement.classList.add('hidden');
            }
            const optionsEl = this.elements[`level${i}Options` as keyof PublishPageElements] as HTMLElement | null;
            if (optionsEl) {
                optionsEl.innerHTML = '';
            }
        }
        
        if (category.children && category.children.length > 0) {
            this.renderCategoryLevel(level + 1, category.children);
            this.selectedCategory = null;
            if (this.elements.selectedCategoryId) {
                this.elements.selectedCategoryId.value = '';
            }
        } else {
            this.selectedCategory = category;
            if (this.elements.selectedCategoryId) {
                this.elements.selectedCategoryId.value = String(category.id);
            }
            this.clearError('category');
            this.updateSelectedCategoryDisplay(category);
            this.hasUnsavedChanges = true;
            this.updateProgress();
        }
    }

    private updateSelectedCategoryDisplay(category: Category): void {
        const categoryPath = this.getCategoryPath(category.id);
        const displayElement = document.getElementById('selectedCategoryDisplay');
        if (displayElement && categoryPath) {
            const span = displayElement.querySelector('span');
            if (span) {
                span.textContent = categoryPath;
            }
            displayElement.classList.add('active');
        }
    }

    private getCategoryPath(categoryId: number, categories: Category[] = this.categories, path = ''): string | null {
        for (const category of categories) {
            const currentPath = path ? `${path} > ${category.name}` : category.name;
            if (category.id === categoryId) {
                return currentPath;
            }
            if (category.children && category.children.length > 0) {
                const found = this.getCategoryPath(categoryId, category.children, currentPath);
                if (found) {return found;}
            }
        }
        return null;
    }

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

    private handleDragOver(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.add('dragover');
    }

    private handleDragLeave(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.remove('dragover');
    }

    private handleDrop(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.remove('dragover');
        
        const files = e.dataTransfer?.files;
        if (files) {
            this.processFiles(Array.from(files));
        }
    }

    private handleFileSelect(e: Event): void {
        const target = e.target as HTMLInputElement;
        const files = target.files;
        if (files) {
            this.processFiles(Array.from(files));
        }
    }

    private processFiles(files: File[]): void {
        const validFiles: File[] = [];
        
        for (const file of files) {
            if (this.images.length + validFiles.length >= this.maxImages) {
                toast.warning(`最多上传${this.maxImages}张图片`);
                break;
            }
            
            if (!file.type.match(/^image\/(jpeg|png|gif|webp)$/)) {
                toast.error('只支持 JPG、PNG、GIF、WEBP 格式的图片');
                continue;
            }
            
            if (file.size > this.maxFileSize) {
                toast.error('图片大小不能超过 5MB');
                continue;
            }
            
            validFiles.push(file);
        }
        
        validFiles.forEach(file => this.addImage(file));
    }

    private addImage(file: File): void {
        const reader = new FileReader();
        
        reader.onload = (e: ProgressEvent<FileReader>) => {
            const imageData: ImageData = {
                id: Date.now() + Math.random(),
                file: file,
                url: e.target?.result as string,
                uploaded: false,
                serverUrl: null
            };
            
            this.images.push(imageData);
            this.renderImagePreviews();
            this.clearError('image');
            this.hasUnsavedChanges = true;
            this.updateProgress();
        };
        
        reader.readAsDataURL(file);
    }

    private renderImagePreviews(): void {
        if (!this.elements.previewList) {return;}
        this.elements.previewList.innerHTML = '';
        
        this.images.forEach((image, index) => {
            const item = document.createElement('div');
            item.className = 'preview-item';
            item.draggable = true;
            item.dataset.id = String(image.id);
            item.setAttribute('role', 'listitem');
            item.innerHTML = `
                <img src="${image.url}" alt="预览图片 ${index + 1}">
                ${index === 0 ? '<span class="cover-badge">封面</span>' : ''}
                <button type="button" class="preview-remove" aria-label="删除图片">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"/>
                        <line x1="6" y1="6" x2="18" y2="18"/>
                    </svg>
                </button>
            `;
            
            const removeBtn = item.querySelector('.preview-remove');
            removeBtn?.addEventListener('click', (e: Event) => {
                e.stopPropagation();
                this.removeImage(image.id);
            });
            
            item.addEventListener('dragstart', (e: DragEvent) => this.handleImageDragStart(e, image.id));
            item.addEventListener('dragend', (e: DragEvent) => this.handleImageDragEnd(e));
            item.addEventListener('dragover', (e: DragEvent) => this.handleImageDragOver(e));
            item.addEventListener('drop', (e: DragEvent) => this.handleImageDrop(e, image.id));
            
            this.elements.previewList?.appendChild(item);
        });
        
        if (this.elements.previewContainer) {
            this.elements.previewContainer.style.display = this.images.length > 0 ? 'block' : 'none';
        }
    }

    private handleImageDragStart(e: DragEvent, id: number): void {
        this.draggedItem = id;
        const target = e.target as HTMLElement;
        target.classList.add('dragging');
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = 'move';
        }
    }

    private handleImageDragEnd(e: DragEvent): void {
        const target = e.target as HTMLElement;
        target.classList.remove('dragging');
        this.draggedItem = null;
        
        document.querySelectorAll('.preview-item').forEach(item => {
            item.classList.remove('drag-over');
        });
    }

    private handleImageDragOver(e: DragEvent): void {
        e.preventDefault();
        if (e.dataTransfer) {
            e.dataTransfer.dropEffect = 'move';
        }
        const target = e.target as HTMLElement;
        target.closest('.preview-item')?.classList.add('drag-over');
    }

    private handleImageDrop(e: DragEvent, targetId: number): void {
        e.preventDefault();
        
        if (!this.draggedItem || this.draggedItem === targetId) {return;}
        
        const draggedIndex = this.images.findIndex(img => img.id === this.draggedItem);
        const targetIndex = this.images.findIndex(img => img.id === targetId);
        
        if (draggedIndex !== -1 && targetIndex !== -1) {
            const [removed] = this.images.splice(draggedIndex, 1);
            this.images.splice(targetIndex, 0, removed);
            this.renderImagePreviews();
            this.hasUnsavedChanges = true;
        }
    }

    private removeImage(id: number): void {
        this.images = this.images.filter(img => img.id !== id);
        this.renderImagePreviews();
        this.hasUnsavedChanges = true;
        this.updateProgress();
    }

    private validateName(): boolean {
        const name = this.elements.productName?.value.trim() || '';
        const wrapper = this.elements.productName?.closest('.input-wrapper');
        
        if (!name) {
            this.showError('name', '请输入商品名称');
            this.elements.productName?.classList.remove('valid');
            this.elements.productName?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (name.length < 2) {
            this.showError('name', '商品名称至少2个字符');
            this.elements.productName?.classList.remove('valid');
            this.elements.productName?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (name.length > 50) {
            this.showError('name', '商品名称最多50个字符');
            this.elements.productName?.classList.remove('valid');
            this.elements.productName?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('name');
        this.elements.productName?.classList.remove('invalid');
        this.elements.productName?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateDescription(): boolean {
        const desc = this.elements.productDescription?.value.trim() || '';
        const wrapper = this.elements.productDescription?.closest('.input-wrapper');
        
        if (!desc) {
            this.showError('desc', '请输入商品描述');
            this.elements.productDescription?.classList.remove('valid');
            this.elements.productDescription?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (desc.length < 10) {
            this.showError('desc', '商品描述至少10个字符');
            this.elements.productDescription?.classList.remove('valid');
            this.elements.productDescription?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (desc.length > 500) {
            this.showError('desc', '商品描述最多500个字符');
            this.elements.productDescription?.classList.remove('valid');
            this.elements.productDescription?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('desc');
        this.elements.productDescription?.classList.remove('invalid');
        this.elements.productDescription?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateCategory(): boolean {
        if (!this.selectedCategory) {
            this.showError('category', '请选择商品分类');
            return false;
        }
        
        this.clearError('category');
        return true;
    }

    private validateImages(): boolean {
        if (this.images.length === 0) {
            this.showError('image', '请至少上传一张商品图片');
            return false;
        }
        
        this.clearError('image');
        return true;
    }

    private validatePrice(): boolean {
        const price = parseFloat(this.elements.salePrice?.value || '0');
        const wrapper = this.elements.salePrice?.closest('.price-input-wrapper');
        
        if (!price || price <= 0) {
            this.showError('price', '请输入有效的出售价格');
            this.elements.salePrice?.classList.remove('valid');
            this.elements.salePrice?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        const originalPrice = parseFloat(this.elements.originalPrice?.value || '0');
        if (originalPrice && originalPrice <= price) {
            this.showError('price', '原价应大于出售价格');
            this.elements.salePrice?.classList.remove('valid');
            this.elements.salePrice?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('price');
        this.elements.salePrice?.classList.remove('invalid');
        this.elements.salePrice?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateStock(): boolean {
        const stock = parseInt(this.elements.stockQuantity?.value || '0');
        const wrapper = this.elements.stockQuantity?.closest('.stock-input-wrapper');
        
        if (!stock || stock < 1) {
            this.showError('stock', '请输入有效的库存数量');
            this.elements.stockQuantity?.classList.remove('valid');
            this.elements.stockQuantity?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (stock > 9999) {
            this.showError('stock', '库存数量不能超过9999');
            this.elements.stockQuantity?.classList.remove('valid');
            this.elements.stockQuantity?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('stock');
        this.elements.stockQuantity?.classList.remove('invalid');
        this.elements.stockQuantity?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateLocation(): boolean {
        const location = this.elements.location?.value.trim() || '';
        const wrapper = this.elements.location?.closest('.input-wrapper');
        
        if (!location) {
            this.showError('location', '请输入交易地点');
            this.elements.location?.classList.remove('valid');
            this.elements.location?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (location.length > 100) {
            this.showError('location', '交易地点最多100个字符');
            this.elements.location?.classList.remove('valid');
            this.elements.location?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('location');
        this.elements.location?.classList.remove('invalid');
        this.elements.location?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateContact(): boolean {
        const contact = this.elements.contactMethod?.value.trim() || '';
        const wrapper = this.elements.contactMethod?.closest('.input-wrapper');
        
        if (!contact) {
            this.showError('contact', '请输入联系方式');
            this.elements.contactMethod?.classList.remove('valid');
            this.elements.contactMethod?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        if (contact.length > 50) {
            this.showError('contact', '联系方式最多50个字符');
            this.elements.contactMethod?.classList.remove('valid');
            this.elements.contactMethod?.classList.add('invalid');
            wrapper?.classList.remove('has-success');
            return false;
        }
        
        this.clearError('contact');
        this.elements.contactMethod?.classList.remove('invalid');
        this.elements.contactMethod?.classList.add('valid');
        wrapper?.classList.add('has-success');
        return true;
    }

    private validateForm(): boolean {
        const validations = [
            this.validateCategory(),
            this.validateName(),
            this.validateDescription(),
            this.validateImages(),
            this.validatePrice(),
            this.validateStock(),
            this.validateLocation(),
            this.validateContact()
        ];
        
        return validations.every(v => v);
    }

    private showError(field: string, message: string): void {
        const errorElement = this.elements[`${field}Error` as keyof PublishPageElements] as HTMLElement | null;
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }
    }

    private clearError(field: string): void {
        const errorElement = this.elements[`${field}Error` as keyof PublishPageElements] as HTMLElement | null;
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }
    }

    private async uploadImages(): Promise<string[] | null> {
        const uploadedUrls: string[] = [];

        for (const image of this.images) {
            if (image.file) {
                try {
                    const formData = new FormData();
                    formData.append('file', image.file);

                    const response = await fetch('/api/file/upload', {
                        method: 'POST',
                        body: formData,
                        headers: {
                            'Authorization': `Bearer ${storage.get<string>('token')}`
                        }
                    });

                    const result: UploadResponse = await response.json();

                    if (isSuccessCode(result.code) && result.data?.url) {
                        uploadedUrls.push(result.data.url);
                        image.uploaded = true;
                        image.serverUrl = result.data.url;
                    } else {
                        throw new Error(result.message || '上传失败');
                    }
                } catch (err) {
                    const errorMessage = err instanceof Error ? err.message : '未知错误';
                    toast.error(`图片上传失败: ${errorMessage}`);
                    return null;
                }
            }
        }

        return uploadedUrls;
    }

    private async handleSubmit(e: Event): Promise<void> {
        e.preventDefault();
        
        if (!this.checkLoginStatus()) {
            return;
        }
        
        if (!this.validateForm()) {
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
            const imageUrls = await this.uploadImages();
            
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
            
            const createRequest: CreateProductRequest = {
                categoryId: this.selectedCategory?.id ?? 0,
                title: this.elements.productName?.value.trim() ?? '',
                description: this.elements.productDescription?.value.trim() ?? '',
                price: parseFloat(this.elements.salePrice?.value ?? '0'),
                originalPrice: parseFloat(this.elements.originalPrice?.value || '0') || undefined,
                condition: this.conditionLevel === 1 ? 'NEW' : this.conditionLevel === 2 ? 'LIKE_NEW' : this.conditionLevel === 3 ? 'GOOD' : 'FAIR',
                location: this.elements.location?.value.trim() ?? '',
                images: imageUrls
            };
            
            const response = await api.product.createProduct(createRequest);
            
            if (isSuccessCode(response.code)) {
                toast.success('商品发布成功！');
                this.clearDraft();
                this.hasUnsavedChanges = false;

                setTimeout(() => {
                    navigation.replace('products');
                }, 1500);
            } else {
                throw new Error(response.message || '发布失败');
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

    private saveDraft(silent = false): void {
        const draft: DraftData = {
            categoryId: this.selectedCategory?.id || null,
            name: this.elements.productName?.value || '',
            description: this.elements.productDescription?.value || '',
            price: this.elements.salePrice?.value || '',
            originalPrice: this.elements.originalPrice?.value || '',
            stock: this.elements.stockQuantity?.value || '',
            conditionLevel: this.conditionLevel,
            location: this.elements.location?.value || '',
            contactMethod: this.elements.contactMethod?.value || '',
            savedAt: new Date().toISOString()
        };
        
        storage.set('publish_draft', draft);
        
        if (!silent) {
            toast.success('草稿已保存');
        }
        
        this.hasUnsavedChanges = false;
    }

    private loadDraft(): void {
        const draft = storage.get<DraftData>('publish_draft');
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
                const value = opt.getAttribute('data-value');
                const isSelected = value ? parseInt(value) === draft.conditionLevel : false;
                opt.classList.toggle('selected', isSelected);
                opt.setAttribute('aria-checked', String(isSelected));
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

    private clearDraft(): void {
        storage.remove('publish_draft');
    }

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
    
    public destroy(): void {
        if (this.autoSaveInterval) {
            clearInterval(this.autoSaveInterval);
            this.autoSaveInterval = null;
        }
    }
}

// ============================================
// 初始化
// ============================================

let publishPageInstance: PublishPage | null = null;

document.addEventListener('DOMContentLoaded', () => {
    publishPageInstance = new PublishPage();
    
    window.addEventListener('beforeunload', () => {
        if (publishPageInstance) {
            publishPageInstance.destroy();
            publishPageInstance = null;
        }
    });
});

export { PublishPage };
export type { ImageData, PublishPageElements, DraftData, ProductCreateData, UploadResponse };
export default PublishPage;
