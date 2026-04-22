/**
 * @fileoverview 工具函数模块 - 统一导出
 * @description 从子模块重新导出所有工具函数，保持向后兼容
 */

// Re-export from sub-modules
export { dom, DomUtils } from './dom.js';
export { toast, ToastUtils } from './toast.js';
export { storage, StorageUtils } from './storage.js';
export { network, NetworkUtils } from './network.js';
export { errorHandler } from './errorHandler.js';
export type { ErrorTypeValue, ErrorSeverityValue } from './errorHandler.js';

export { validator, ValidatorUtils } from './validator.js';

export { eventManager, EventManager } from './eventManager.js';
export { modalManager, ModalManager } from './modalManager.js';
export { FormManager, FormValidators } from './formManager.js';
export type { FormField, ValidationResult, SubmitOptions, FieldValidator } from './formManager.js';

export { debounce, throttle } from './function.js';
export type { DebounceOptions } from './function.js';

export {
    formatCurrency,
    formatDate,
    formatRelativeTime,
    escapeHtml,
    parseQueryString,
    buildQueryString
} from './format.js';

export {
    createElement,
    createImage,
    createButton,
    createSvgIcon,
    createFragment,
    appendChildren,
    clearChildren,
    createEmptyState
} from './element.js';
export type { ElementOptions } from './element.js';

export {
    calculateDiscount,
    getConditionNameFromString,
    isHotProduct
} from './product.js';

// Re-export types
export type { ToastType } from '../types/index.js';
export type { Unsubscribe, NetworkChangeCallback } from './network.js';
export type { StorageItem } from '../types/index.js';
