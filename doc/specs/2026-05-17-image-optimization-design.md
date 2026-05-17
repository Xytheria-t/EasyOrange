# 图片优化设计文档

> 日期: 2026-05-17
> 状态: 待实施

## 概述

EasyOrange 现有完整的图片基础设施（上传、缩略图、响应式图片、WebP 自适应），但在存储架构、裁剪策略和压缩质量方面存在可优化空间。本文档设计三个优化模块，全部增量改造，不改前端 API。

## 背景

### 现有能力

- 后端: Thumbnailator 图片处理、本地文件存储、Caffeine 图片缓存、ETag 304
- 前端: 上传前压缩 (max 1600×1600, quality 0.8)、`Image` 组件（响应式/WebP/懒加载）
- 商品图片: `eo_product_image` 表，上传后存 URL，支持多图排序/封面标记

### 待优化点

1. **存储紧耦合本地磁盘** — 无可插拔存储抽象，无法接入云存储/CDN
2. **无智能裁剪** — 仅支持中心裁剪，可能裁掉商品主体
3. **压缩质量硬编码** — 不支持渐进式 JPEG，质量值不可配置

## 模块一：可插拔存储抽象层

### 核心接口

在 `easyorange-framework` 的 `com.cartethyia.easyorange.framework.file.storage` 包下：

```java
public interface FileStorage {
    /** 保存文件，返回存储标识符 */
    String store(byte[] content, String originalFilename, String contentType) throws IOException;
    /** 读取文件 */
    byte[] load(String identifier) throws IOException;
    /** 删除文件 */
    void delete(String identifier) throws IOException;
    /** 获取外部可访问的 URL */
    String getUrl(String identifier);
    /** 是否支持直接 URL 访问（本地否，OSS 是） */
    boolean supportsDirectUrl();
}
```

### 实现

| 实现类 | 说明 | 优先级 |
|--------|------|--------|
| `LocalFileStorage` | 从现有 `FileServiceImpl` 提取文件 IO 逻辑 | 本次实现 |
| `S3FileStorage` | S3 兼容协议（MinIO/AWS S3） | 后续 |
| `AliyunOssFileStorage` | 阿里云 OSS | 后续 |

### 集成

```
FileController → FileServiceImpl → FileStorage (interface)
                                     ├── LocalFileStorage (当前)
                                     ├── S3FileStorage (未来)
                                     └── AliyunOssFileStorage (未来)
```

- `FileServiceImpl` 改为 `FileStorage` 的编排层（存储 + 图片处理 + 元数据）
- `AvatarFilePort.LocalAvatarFileStorage` 继续委托 `FileService`，不感知存储层
- 前端 API 不变

### 数据库变更

`eo_upload_file` 表新增字段：

| 字段 | 类型 | 说明 |
|------|------|------|
| `storage_type` | VARCHAR(32) | `LOCAL` / `S3` / `OSS`，默认 `LOCAL` |
| `storage_key` | VARCHAR(500) | 存储后端的标识键 |

### 配置

```yaml
easyorange:
  file:
    storage:
      type: local  # local | s3 | oss
      local:
        upload-path: ./upload
```

## 模块二：服务端智能裁剪

### 算法

基于图像 Shannon Entropy 的裁剪，**零额外依赖**：

1. 将图片划分为滑动窗口网格（步长 = 窗口宽度 25%）
2. 对每个窗口计算 `Entropy = -Σ(p * log₂(p))`（p = 灰度直方图归一化值）
3. 选取熵值最高的区域作为裁剪框
4. 如最高熵 < 阈值（纯色背景），回退中心裁剪

### 接口

```java
public interface ImageProcessingService {
    // 已有方法...

    /** 智能裁剪：识别信息最丰富区域并裁剪到目标比例 */
    BufferedImage smartCrop(BufferedImage source, int targetWidth, int targetHeight);

    /** 带回退：图片小于目标尺寸时跳过 */
    BufferedImage smartCropWithFallback(BufferedImage source, int targetWidth, int targetHeight);
}
```

### 集成点

- 上传时自动裁剪（配置 `smart-crop.enabled=true` 时）
- `/api/file/upload` 可选参数 `?crop=1:1`
- `/view` 和 `/thumbnail` 端点后续复用

### 配置

```yaml
easyorange:
  file:
    image:
      smart-crop:
        enabled: true
        default-aspect-ratio: 1:1
        min-entropy-threshold: 0.5
```

## 模块三：压缩优化

### 渐进式 JPEG

- 图片 ≥ 100KB 且输出 JPEG 时，启用渐进编码
- 小图片继续使用 Baseline JPEG（兼容性更好）

### 质量配置化

| 配置项 | 默认值 | 说明 |
|--------|--------|------|
| `quality.default` | 0.80 | 默认输出质量 |
| `quality.thumbnail` | 0.75 | 缩略图质量 |
| `quality.responsive` | 0.75 | 响应式图片质量 |
| `progressive-jpeg.enabled` | true | 渐进式开关 |
| `progressive-jpeg.min-size` | 102400 | 渐进式阈值（字节） |

### 涉及文件

| 文件 | 变更内容 |
|------|----------|
| `ImageProcessingService.java` | 新增带 quality 参数的重载方法 |
| `ImageProcessingServiceImpl.java` | 注入配置、替换硬编码、渐进式 JPEG 逻辑 |
| `application.yaml` | 新增配置项 |
| `LocalCacheConfig.java` | 缓存 key 纳入 quality 参数 |

## 不涉及变更

- 前端 API 不变
- 前端 `Image.tsx`、`imageCompress.ts` 不变
- 商品图片存储流程不变
- 用户头像上传流程不变
- `ImageUrl` 值对象验证规则不变

## 实施顺序

1. **模块三（压缩优化）** — 改动最小，无架构变更，快速交付
2. **模块二（智能裁剪）** — 纯新增算法+配置，独立可测
3. **模块一（存储抽象）** — 涉及重构，需要更充分的测试
