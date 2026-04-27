/**
 * @fileoverview 发布页面常量定义
 * @version 2.0.0
 */

/** 最大图片数量 */
export const MAX_IMAGES = 9;

/** 最大文件大小（字节） */
export const MAX_FILE_SIZE: number = 5 * 1024 * 1024;

/** 自动保存间隔（毫秒） */
export const AUTO_SAVE_INTERVAL = 30000;

/** 允许的图片类型 */
export const ALLOWED_IMAGE_TYPES: readonly string[] = [
    'image/jpeg',
    'image/png',
    'image/gif',
    'image/webp'
] as const;

/** 图片类型错误提示 */
export const IMAGE_TYPE_ERROR = '只支持 JPG、PNG、GIF、WEBP 格式的图片';

/** 文件大小错误提示 */
export const FILE_SIZE_ERROR = '图片大小不能超过 5MB';

/** 最大图片数量错误提示 */
export const MAX_IMAGES_ERROR = '最多上传9张图片';
