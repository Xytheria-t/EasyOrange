/**
 * @fileoverview 表单验证模块
 * @version 2.0.0
 */

/** 验证字段类型 */
export type ValidationField = 'name' | 'desc' | 'price' | 'stock' | 'location' | 'contact';

/** 验证错误映射 */
export type ValidationErrors = Record<ValidationField, string>;

/** 表单元素接口 */
export interface FormElements {
    productName: HTMLInputElement | null;
    productDescription: HTMLTextAreaElement | null;
    salePrice: HTMLInputElement | null;
    originalPrice: HTMLInputElement | null;
    stockQuantity: HTMLInputElement | null;
    location: HTMLInputElement | null;
    contactMethod: HTMLInputElement | null;
    nameError: HTMLElement | null;
    descError: HTMLElement | null;
    priceError: HTMLElement | null;
    stockError: HTMLElement | null;
    locationError: HTMLElement | null;
    contactError: HTMLElement | null;
}

/**
 * 表单验证器类
 * 负责发布页面表单的验证逻辑
 */
export class FormValidator {
    private errors: Partial<ValidationErrors>;
    private elements: FormElements;

    /**
     * 创建表单验证器实例
     */
    constructor() {
        this.errors = {};
        this.elements = {
            productName: null,
            productDescription: null,
            salePrice: null,
            originalPrice: null,
            stockQuantity: null,
            location: null,
            contactMethod: null,
            nameError: null,
            descError: null,
            priceError: null,
            stockError: null,
            locationError: null,
            contactError: null
        };
    }

    /**
     * 初始化表单元素引用
     */
    initElements(): void {
        this.elements = {
            productName: document.getElementById('productName') as HTMLInputElement | null,
            productDescription: document.getElementById('productDescription') as HTMLTextAreaElement | null,
            salePrice: document.getElementById('salePrice') as HTMLInputElement | null,
            originalPrice: document.getElementById('originalPrice') as HTMLInputElement | null,
            stockQuantity: document.getElementById('stockQuantity') as HTMLInputElement | null,
            location: document.getElementById('location') as HTMLInputElement | null,
            contactMethod: document.getElementById('contactMethod') as HTMLInputElement | null,
            nameError: document.getElementById('nameError'),
            descError: document.getElementById('descError'),
            priceError: document.getElementById('priceError'),
            stockError: document.getElementById('stockError'),
            locationError: document.getElementById('locationError'),
            contactError: document.getElementById('contactError')
        };
    }

    /**
     * 验证商品名称
     * @returns 验证是否通过
     */
    validateName(): boolean {
        const name = this.elements.productName?.value.trim() || '';
        const wrapper = this.elements.productName?.closest('.input-wrapper');

        if (!name) {
            this.showError('name', '请输入商品名称');
            return false;
        }

        if (name.length < 2) {
            this.showError('name', '商品名称至少2个字符');
            return false;
        }

        if (name.length > 50) {
            this.showError('name', '商品名称最多50个字符');
            return false;
        }

        this.clearError('name');
        this.showSuccess(this.elements.productName, wrapper);
        return true;
    }

    /**
     * 验证商品描述
     * @returns 验证是否通过
     */
    validateDescription(): boolean {
        const desc = this.elements.productDescription?.value.trim() || '';
        const wrapper = this.elements.productDescription?.closest('.input-wrapper');

        if (!desc) {
            this.showError('desc', '请输入商品描述');
            return false;
        }

        if (desc.length < 10) {
            this.showError('desc', '商品描述至少10个字符');
            return false;
        }

        if (desc.length > 500) {
            this.showError('desc', '商品描述最多500个字符');
            return false;
        }

        this.clearError('desc');
        this.showSuccess(this.elements.productDescription, wrapper);
        return true;
    }

    /**
     * 验证价格
     * @returns 验证是否通过
     */
    validatePrice(): boolean {
        const price = parseFloat(this.elements.salePrice?.value || '0');
        const wrapper = this.elements.salePrice?.closest('.price-input-wrapper');

        if (!price || price <= 0) {
            this.showError('price', '请输入有效的出售价格');
            return false;
        }

        const originalPrice = parseFloat(this.elements.originalPrice?.value || '0');
        if (originalPrice && originalPrice <= price) {
            this.showError('price', '原价应大于出售价格');
            return false;
        }

        this.clearError('price');
        this.showSuccess(this.elements.salePrice, wrapper);
        return true;
    }

    /**
     * 验证库存
     * @returns 验证是否通过
     */
    validateStock(): boolean {
        const stock = parseInt(this.elements.stockQuantity?.value || '0', 10);
        const wrapper = this.elements.stockQuantity?.closest('.stock-input-wrapper');

        if (!stock || stock < 1) {
            this.showError('stock', '请输入有效的库存数量');
            return false;
        }

        if (stock > 9999) {
            this.showError('stock', '库存数量不能超过9999');
            return false;
        }

        this.clearError('stock');
        this.showSuccess(this.elements.stockQuantity, wrapper);
        return true;
    }

    /**
     * 验证交易地点
     * @returns 验证是否通过
     */
    validateLocation(): boolean {
        const location = this.elements.location?.value.trim() || '';
        const wrapper = this.elements.location?.closest('.input-wrapper');

        if (!location) {
            this.showError('location', '请输入交易地点');
            return false;
        }

        if (location.length > 100) {
            this.showError('location', '交易地点最多100个字符');
            return false;
        }

        this.clearError('location');
        this.showSuccess(this.elements.location, wrapper);
        return true;
    }

    /**
     * 验证联系方式
     * @returns 验证是否通过
     */
    validateContact(): boolean {
        const contact = this.elements.contactMethod?.value.trim() || '';
        const wrapper = this.elements.contactMethod?.closest('.input-wrapper');

        if (!contact) {
            this.showError('contact', '请输入联系方式');
            return false;
        }

        if (contact.length > 50) {
            this.showError('contact', '联系方式最多50个字符');
            return false;
        }

        this.clearError('contact');
        this.showSuccess(this.elements.contactMethod, wrapper);
        return true;
    }

    /**
     * 显示错误信息
     * @param field - 字段名
     * @param message - 错误信息
     */
    private showError(field: ValidationField, message: string): void {
        this.errors[field] = message;
        const errorElement = this.elements[`${field}Error` as keyof FormElements] as HTMLElement | null;
        if (errorElement) {
            errorElement.textContent = message;
            errorElement.style.display = 'block';
        }

        const inputElement = this.getInputByField(field);
        if (inputElement) {
            inputElement.classList.remove('valid');
            inputElement.classList.add('invalid');
        }
    }

    /**
     * 清除错误信息
     * @param field - 字段名
     */
    private clearError(field: ValidationField): void {
        delete this.errors[field];
        const errorElement = this.elements[`${field}Error` as keyof FormElements] as HTMLElement | null;
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.style.display = 'none';
        }

        const inputElement = this.getInputByField(field);
        if (inputElement) {
            inputElement.classList.remove('invalid');
            inputElement.classList.add('valid');
        }
    }

    /**
     * 根据字段名获取输入元素
     * @param field - 字段名
     * @returns 输入元素
     */
    private getInputByField(field: ValidationField): HTMLInputElement | HTMLTextAreaElement | null {
        const fieldMap: Record<ValidationField, keyof FormElements> = {
            stock: 'stockQuantity',
            price: 'salePrice',
            name: 'productName',
            desc: 'productDescription',
            location: 'location',
            contact: 'contactMethod'
        };
        return this.elements[fieldMap[field]] as HTMLInputElement | HTMLTextAreaElement | null;
    }

    /**
     * 显示成功状态
     * @param inputElement - 输入元素
     * @param wrapper - 包装元素
     */
    private showSuccess(
        inputElement: HTMLInputElement | HTMLTextAreaElement | null,
        wrapper: Element | null | undefined
    ): void {
        if (inputElement) {
            inputElement.classList.remove('invalid');
            inputElement.classList.add('valid');
        }
        if (wrapper) {
            wrapper.classList.add('has-success');
        }
    }

    /**
     * 验证所有字段
     * @returns 所有验证是否通过
     */
    validateAll(): boolean {
        const results: boolean[] = [
            this.validateName(),
            this.validateDescription(),
            this.validatePrice(),
            this.validateStock(),
            this.validateLocation(),
            this.validateContact()
        ];

        return results.every(r => r);
    }

    /**
     * 获取所有错误
     * @returns 错误映射
     */
    getErrors(): Partial<ValidationErrors> {
        return this.errors;
    }

    /**
     * 检查是否存在错误
     * @returns 是否存在错误
     */
    hasErrors(): boolean {
        return Object.keys(this.errors).length > 0;
    }
}

export default FormValidator;
