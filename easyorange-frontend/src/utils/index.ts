/**
 * @fileoverview 工具函数模块 - React 版本
 * @description 只提供 React 应用需要的工具函数
 */

// 类名工具
export { cn } from './cn';
// 错误处理
export { errorHandler } from './errorHandler';

// 格式化工具
export {
    buildQueryString,
    escapeHtml,
    formatCondition,
    formatCurrency,
    formatDate,
    formatPrice,
    formatRelativeTime,
    parseQueryString,
} from './format';
export type { DebounceOptions } from './functionUtils';

// 函数工具
export { debounce, throttle } from './functionUtils';
// 商品工具
export { calculateDiscount, getConditionNameFromString, normalizeProduct } from './product';
export type { StorageItem } from './storage';
// 存储工具
export { StorageUtils, storage } from './storage';
