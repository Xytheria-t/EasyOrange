/**
 * @fileoverview 工具函数模块 - React 版本
 * @description 只提供 React 应用需要的工具函数
 */

// 类名工具
export { cn } from '@/lib/utils';
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

// 函数工具
export { debounce, throttle } from './functionUtils';
// 商品工具
export { calculateDiscount, getConditionNameFromString, normalizeProduct } from './product';
// 存储工具
export { storage } from './storage';
