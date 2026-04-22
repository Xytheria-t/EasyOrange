/**
 * @fileoverview 统一表单管理模块
 * @description 提供表单验证、提交、重置的统一管理
 * @version 1.0.0
 */

import { validator } from './validator.js';

interface FormField {
    name: string;
    value: string;
    required?: boolean;
    validators?: Array<(value: string) => string | null>;
}

interface ValidationResult {
    isValid: boolean;
    errors: Record<string, string>;
}

interface SubmitOptions {
    beforeSubmit?: () => boolean | Promise<boolean>;
    afterSubmit?: (response?: unknown) => void | Promise<void>;
    onError?: (error: Error) => void;
}

/**
 * 表单管理器
 * 提供统一的表单验证和提交功能
 */
class FormManager {
    private formElement: HTMLFormElement | null;
    private validationRules: Map<string, FieldValidator>;
    private submittedFields: Set<string>;

    constructor(formElement?: HTMLFormElement) {
        this.formElement = formElement || null;
        this.validationRules = new Map();
        this.submittedFields = new Set();
    }

    /**
     * 设置表单元素
     * @param formElement - 表单元素
     */
    setForm(formElement: HTMLFormElement): void {
        this.formElement = formElement;
    }

    /**
     * 添加验证规则
     * @param fieldName - 字段名
     * @param rules - 验证规则
     */
    addValidationRules(fieldName: string, rules: FieldValidator): void {
        this.validationRules.set(fieldName, rules);
    }

    /**
     * 验证单个字段
     * @param fieldName - 字段名
     * @param value - 字段值
     */
    validateField(fieldName: string, value: string): string | null {
        const rules = this.validationRules.get(fieldName);
        if (!rules) {return null;}

        // 必填验证
        if (rules.required && (!value || value.trim() === '')) {
            return rules.requiredMessage || `${fieldName}不能为空`;
        }

        // 自定义验证器
        if (rules.validators) {
            for (const validateFn of rules.validators) {
                const error = validateFn(value);
                if (error) {return error;}
            }
        }

        return null;
    }

    /**
     * 验证所有字段
     * @param fields - 要验证的字段列表
     */
    validateFields(fields: FormField[]): ValidationResult {
        const errors: Record<string, string> = {};
        let isValid = true;

        for (const field of fields) {
            const error = this.validateField(field.name, field.value);
            if (error) {
                errors[field.name] = error;
                isValid = false;
            }
        }

        return { isValid, errors };
    }

    /**
     * 验证整个表单
     */
    validateForm(): ValidationResult {
        if (!this.formElement) {
            return { isValid: false, errors: { form: '表单未设置' } };
        }

        const formData = this.getFormData();
        return this.validateFields(formData);
    }

    /**
     * 获取表单数据
     */
    getFormData(): FormField[] {
        if (!this.formElement) {return [];}

        const formData: FormField[] = [];
        const elements = this.formElement.elements;

        for (const element of elements) {
            if (element instanceof HTMLInputElement ||
                element instanceof HTMLSelectElement ||
                element instanceof HTMLTextAreaElement) {
                const name = element.name;
                if (!name) {continue;}

                formData.push({
                    name,
                    value: element.value,
                    required: element.required
                });
            }
        }

        return formData;
    }

    /**
     * 获取表单数据为对象
     */
    getFormDataAsObject(): Record<string, string> {
        const formData = this.getFormData();
        const result: Record<string, string> = {};

        for (const field of formData) {
            result[field.name] = field.value;
        }

        return result;
    }

    /**
     * 显示字段错误
     * @param fieldName - 字段名
     * @param errorMessage - 错误消息
     */
    showFieldError(fieldName: string, errorMessage: string): void {
        if (!this.formElement) {return;}

        const errorElement = this.formElement.querySelector(`[data-error-for="${fieldName}"]`);
        if (errorElement) {
            errorElement.textContent = errorMessage;
            errorElement.classList.add('visible');
        }

        const inputElement = this.formElement.querySelector(`[name="${fieldName}"]`);
        if (inputElement) {
            inputElement.classList.add('error');
        }
    }

    /**
     * 清除字段错误
     * @param fieldName - 字段名
     */
    clearFieldError(fieldName: string): void {
        if (!this.formElement) {return;}

        const errorElement = this.formElement.querySelector(`[data-error-for="${fieldName}"]`);
        if (errorElement) {
            errorElement.textContent = '';
            errorElement.classList.remove('visible');
        }

        const inputElement = this.formElement.querySelector(`[name="${fieldName}"]`);
        if (inputElement) {
            inputElement.classList.remove('error');
        }
    }

    /**
     * 清除所有错误
     */
    clearAllErrors(): void {
        if (!this.formElement) {return;}

        const errorElements = this.formElement.querySelectorAll('[data-error-for]');
        errorElements.forEach(el => {
            el.textContent = '';
            el.classList.remove('visible');
        });

        const inputElements = this.formElement.querySelectorAll('.error');
        inputElements.forEach(el => el.classList.remove('error'));
    }

    /**
     * 重置表单
     */
    resetForm(): void {
        if (this.formElement) {
            this.formElement.reset();
        }
        this.clearAllErrors();
        this.submittedFields.clear();
    }

    /**
     * 提交表单
     * @param options - 提交选项
     */
    async submitForm<T>(options: SubmitOptions): Promise<T | null> {
        const { beforeSubmit, afterSubmit, onError } = options;

        // 验证表单
        const validation = this.validateForm();
        if (!validation.isValid) {
            // 显示第一个错误
            const firstErrorField = Object.keys(validation.errors)[0];
            this.showFieldError(firstErrorField, validation.errors[firstErrorField]);
            return null;
        }

        // beforeSubmit 钩子
        if (beforeSubmit) {
            const canContinue = await beforeSubmit();
            if (!canContinue) {return null;}
        }

        try {
            const response = await this.doSubmit();
            if (afterSubmit) {
                await afterSubmit(response);
            }
            return response as T;
        } catch (error) {
            if (onError) {
                onError(error as Error);
            }
            throw error;
        }
    }

    /**
     * 执行实际提交（由子类或实例覆写）
     */
    protected doSubmit(): Promise<unknown> {
        return Promise.resolve(null);
    }

    /**
     * 标记字段已提交（用于显示错误）
     * @param fieldName - 字段名
     */
    markAsSubmitted(fieldName: string): void {
        this.submittedFields.add(fieldName);
    }

    /**
     * 检查字段是否已提交
     * @param fieldName - 字段名
     */
    isSubmitted(fieldName: string): boolean {
        return this.submittedFields.has(fieldName);
    }
}

/**
 * 字段验证器接口
 */
interface FieldValidator {
    required?: boolean;
    requiredMessage?: string;
    validators?: Array<(value: string) => string | null>;
}

/**
 * 常用验证器工厂
 */
const FormValidators = {
    /**
     * 邮箱验证
     */
    email: (message?: string) => (value: string) => {
        if (value && !validator.isValidEmail(value)) {
            return message || '请输入有效的邮箱地址';
        }
        return null;
    },

    /**
     * 密码验证
     */
    password: (message?: string) => (value: string) => {
        if (value && !validator.isStrongPassword(value)) {
            return message || '密码至少需要 6 个字符';
        }
        return null;
    },

    /**
     * 手机号验证
     */
    phone: (message?: string) => (value: string) => {
        if (value && !validator.isValidPhone(value)) {
            return message || '请输入有效的手机号';
        }
        return null;
    },

    /**
     * 学号验证
     */
    studentId: (message?: string) => (value: string) => {
        if (value && !validator.isValidStudentId(value)) {
            return message || '请输入有效的学号';
        }
        return null;
    },

    /**
     * 真实姓名验证
     */
    realName: (message?: string) => (value: string) => {
        if (value && !validator.isValidRealName(value)) {
            return message || '真实姓名至少需要 2 个字符';
        }
        return null;
    },

    /**
     * 最小长度验证
     */
    minLength: (min: number, message?: string) => (value: string) => {
        if (value && value.length < min) {
            return message || `长度至少需要 ${min} 个字符`;
        }
        return null;
    },

    /**
     * 最大长度验证
     */
    maxLength: (max: number, message?: string) => (value: string) => {
        if (value && value.length > max) {
            return message || `长度不能超过 ${max} 个字符`;
        }
        return null;
    },

    /**
     * 确认密码验证
     */
    confirmPassword: (passwordField: string, message?: string) => (value: string, formData?: Record<string, string>) => {
        if (formData && value !== formData[passwordField]) {
            return message || '两次密码输入不一致';
        }
        return null;
    }
};

export { FormManager, FormValidators };
export type { FormField, ValidationResult, SubmitOptions, FieldValidator };
export default FormManager;
