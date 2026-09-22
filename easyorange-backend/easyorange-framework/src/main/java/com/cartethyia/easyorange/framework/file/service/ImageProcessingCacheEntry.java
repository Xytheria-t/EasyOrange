package com.cartethyia.easyorange.framework.file.service;

import java.io.File;

/**
 * 图片处理缓存条目 — {@code ImageProcessCacheConfig} 那个 Caffeine 缓存的值类型。
 * <p>
 * 单独成类型而不是 {@code Object}：缓存 bean 的泛型随之确定，注入点按类型即可唯一解析
 * （不必用 {@code @Qualifier} 字符串限定），读取侧也不必强转。
 *
 * @param file    处理结果落盘的文件
 * @param mimeType 处理后的 MIME 类型
 * @param eTag    内容指纹，用于 304 未修改判定
 */
public record ImageProcessingCacheEntry(File file, String mimeType, String eTag) {}
