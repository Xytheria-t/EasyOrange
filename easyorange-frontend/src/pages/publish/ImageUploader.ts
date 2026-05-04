/**
 * @fileoverview 图片上传模块
 * @version 2.0.0
 */

import {
    MAX_IMAGES,
    MAX_FILE_SIZE,
    ALLOWED_IMAGE_TYPES,
    IMAGE_TYPE_ERROR,
    FILE_SIZE_ERROR,
    MAX_IMAGES_ERROR
} from './constants.js';
import { errorHandler } from '../../utils/errorHandler.js';
import { isSuccessCode } from '../../types';
import { getStoredToken } from '../../features/auth/session.js';

type ToastType = 'success' | 'error' | 'warning' | 'info';

interface ToastMessage {
    type: ToastType;
    message: string;
}

/** 图片数据接口 */
export interface ImageData {
    id: number;
    file: File;
    url: string;
    uploaded: boolean;
    serverUrl: string | null;
}

/** 图片上传器选项 */
export interface ImageUploaderOptions {
    maxImages?: number;
    maxFileSize?: number;
    onImagesChange?: (images: ImageData[]) => void;
    showToast?: (toast: ToastMessage) => void;
}

/** 图片上传器元素接口 */
export interface ImageUploaderElements {
    uploadArea: HTMLElement | null;
    fileInput: HTMLInputElement | null;
    previewContainer: HTMLElement | null;
    previewList: HTMLElement | null;
    imageError: HTMLElement | null;
}

/**
 * 图片上传器类
 * 负责图片的选择、预览、拖拽排序和上传
 */
export class ImageUploader {
    private maxImages: number;
    private maxFileSize: number;
    private images: ImageData[];
    private onImagesChange: (images: ImageData[]) => void;
    private showToast: (toast: ToastMessage) => void;
    private draggedItem: number | null;
    private elements: ImageUploaderElements;

    constructor(options: ImageUploaderOptions = {}) {
        this.maxImages = options.maxImages || MAX_IMAGES;
        this.maxFileSize = options.maxFileSize || MAX_FILE_SIZE;
        this.images = [];
        this.onImagesChange = options.onImagesChange || (() => {});
        this.showToast = options.showToast || ((t) => console.log(`[${t.type}] ${t.message}`));
        this.draggedItem = null;
        this.elements = {
            uploadArea: null,
            fileInput: null,
            previewContainer: null,
            previewList: null,
            imageError: null
        };
        this.initElements();
    }

    /**
     * 初始化元素引用
     */
    private initElements(): void {
        this.elements = {
            uploadArea: document.getElementById('uploadArea'),
            fileInput: document.getElementById('fileInput') as HTMLInputElement | null,
            previewContainer: document.getElementById('previewContainer'),
            previewList: document.getElementById('previewList'),
            imageError: document.getElementById('imageError')
        };
        this.bindEvents();
    }

    /**
     * 绑定事件监听器
     */
    private bindEvents(): void {
        if (this.elements.uploadArea) {
            this.elements.uploadArea.addEventListener('click', () => this.elements.fileInput?.click());
            this.elements.uploadArea.addEventListener('keydown', (e: KeyboardEvent) => {
                if (e.key === 'Enter' || e.key === ' ') {
                    e.preventDefault();
                    this.elements.fileInput?.click();
                }
            });
            this.elements.uploadArea.addEventListener('dragover', (e: DragEvent) => this.handleDragOver(e));
            this.elements.uploadArea.addEventListener('dragleave', (e: DragEvent) => this.handleDragLeave(e));
            this.elements.uploadArea.addEventListener('drop', (e: DragEvent) => this.handleDrop(e));
        }

        if (this.elements.fileInput) {
            this.elements.fileInput.addEventListener('change', (e: Event) => this.handleFileSelect(e));
        }
    }

    /**
     * 处理拖拽进入
     * @param e - 拖拽事件
     */
    private handleDragOver(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.add('dragover');
    }

    /**
     * 处理拖拽离开
     * @param e - 拖拽事件
     */
    private handleDragLeave(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.remove('dragover');
    }

    /**
     * 处理拖拽放下
     * @param e - 拖拽事件
     */
    private handleDrop(e: DragEvent): void {
        e.preventDefault();
        e.stopPropagation();
        this.elements.uploadArea?.classList.remove('dragover');

        const files = e.dataTransfer?.files;
        if (files) {
            this.processFiles(files);
        }
    }

    /**
     * 处理文件选择
     * @param e - 事件
     */
    private handleFileSelect(e: Event): void {
        const target = e.target as HTMLInputElement;
        const files = target.files;
        if (files) {
            this.processFiles(files);
        }
    }

    /**
     * 处理文件列表
     * @param files - 文件列表
     */
    processFiles(files: FileList): void {
        const validFiles: File[] = [];

        for (const file of Array.from(files)) {
            if (this.images.length + validFiles.length >= this.maxImages) {
                this.showToast({ type: 'warning', message: MAX_IMAGES_ERROR });
                break;
            }

            if (!ALLOWED_IMAGE_TYPES.includes(file.type)) {
                this.showToast({ type: 'error', message: IMAGE_TYPE_ERROR });
                continue;
            }

            if (file.size > this.maxFileSize) {
                this.showToast({ type: 'error', message: FILE_SIZE_ERROR });
                continue;
            }

            validFiles.push(file);
        }

        validFiles.forEach(file => this.addImage(file));
    }

    /**
     * 添加图片
     * @param file - 图片文件
     */
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
            this.renderPreviews();
            this.clearError();
            this.onImagesChange(this.images);
        };

        reader.readAsDataURL(file);
    }

    /**
     * 渲染图片预览
     */
    private renderPreviews(): void {
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
            if (this.images.length > 0) {
                this.elements.previewContainer.style.display = 'block';
            } else {
                this.elements.previewContainer.style.display = 'none';
            }
        }
    }

    /**
     * 处理图片拖拽开始
     * @param e - 拖拽事件
     * @param id - 图片ID
     */
    private handleImageDragStart(e: DragEvent, id: number): void {
        this.draggedItem = id;
        const target = e.target as HTMLElement;
        target.classList.add('dragging');
        if (e.dataTransfer) {
            e.dataTransfer.effectAllowed = 'move';
        }
    }

    /**
     * 处理图片拖拽结束
     * @param e - 拖拽事件
     */
    private handleImageDragEnd(e: DragEvent): void {
        const target = e.target as HTMLElement;
        target.classList.remove('dragging');
        this.draggedItem = null;

        document.querySelectorAll('.preview-item').forEach(item => {
            item.classList.remove('drag-over');
        });
    }

    /**
     * 处理图片拖拽悬停
     * @param e - 拖拽事件
     */
    private handleImageDragOver(e: DragEvent): void {
        e.preventDefault();
        if (e.dataTransfer) {
            e.dataTransfer.dropEffect = 'move';
        }
        const target = e.target as HTMLElement;
        target.closest('.preview-item')?.classList.add('drag-over');
    }

    /**
     * 处理图片拖拽放下
     * @param e - 拖拽事件
     * @param targetId - 目标图片ID
     */
    private handleImageDrop(e: DragEvent, targetId: number): void {
        e.preventDefault();

        if (!this.draggedItem || this.draggedItem === targetId) {return;}

        const draggedIndex = this.images.findIndex(img => img.id === this.draggedItem);
        const targetIndex = this.images.findIndex(img => img.id === targetId);

        if (draggedIndex !== -1 && targetIndex !== -1) {
            const [removed] = this.images.splice(draggedIndex, 1);
            this.images.splice(targetIndex, 0, removed);
            this.renderPreviews();
            this.onImagesChange(this.images);
        }
    }

    /**
     * 移除图片
     * @param id - 图片ID
     */
    removeImage(id: number): void {
        this.images = this.images.filter(img => img.id !== id);
        this.renderPreviews();
        this.onImagesChange(this.images);
    }

    /**
     * 上传所有图片
     * @returns 上传成功返回图片URL数组，失败返回null
     */
    async uploadAll(): Promise<string[] | null> {
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
                            'Authorization': `Bearer ${getStoredToken()}`
                        }
                    });

                    const result = await response.json();

                    if (isSuccessCode(result.code) && result.data?.url) {
                        uploadedUrls.push(result.data.url);
                        image.uploaded = true;
                        image.serverUrl = result.data.url;
                    } else {
                        throw new Error(result.message || '上传失败');
                    }
                } catch (error) {
                    const errorMessage = errorHandler.handle(error as Error);
                    this.showToast({ type: 'error', message: `图片上传失败: ${errorMessage}` });
                    return null;
                }
            }
        }

        return uploadedUrls;
    }

    /**
     * 获取所有图片
     * @returns 图片数组
     */
    getImages(): ImageData[] {
        return this.images;
    }

    /**
     * 验证图片
     * @returns 验证是否通过
     */
    validate(): boolean {
        if (this.images.length === 0) {
            if (this.elements.imageError) {
                this.elements.imageError.textContent = '请至少上传一张商品图片';
                this.elements.imageError.style.display = 'block';
            }
            return false;
        }
        this.clearError();
        return true;
    }

    /**
     * 清除错误信息
     */
    private clearError(): void {
        if (this.elements.imageError) {
            this.elements.imageError.textContent = '';
            this.elements.imageError.style.display = 'none';
        }
    }
}

export default ImageUploader;
