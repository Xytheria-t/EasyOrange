import { z } from 'zod';

/**
 * 商品发布表单校验 schema
 *
 * 与 react-hook-form + @hookform/resolvers/zod 配合使用，
 * 也可单独用于 API 层数据校验。
 *
 * 注意：不使用 .default() —— 默认值通过 useForm 的 defaultValues 设置，
 * 避免 input/output 类型不一致问题。
 */
export const publishSchema = z.object({
  name: z
    .string()
    .min(1, '请输入资产名称')
    .max(200, '资产名称最多200个字符'),
  description: z
    .string()
    .max(2000, '描述最多2000个字符'),
  price: z
    .string()
    .min(1, '请输入价格')
    .refine((val) => !isNaN(Number(val)) && Number(val) > 0, '请输入有效价格'),
  originalPrice: z
    .string(),
  categoryId: z
    .string()
    .min(1, '请选择资产类别'),
  conditionLevel: z
    .string()
    .min(1, '请选择新旧程度'),
  stock: z
    .string(),
  location: z
    .string()
    .max(100, '地点最多100个字符'),
  contactMethod: z
    .string()
    .max(50, '联系方式最多50个字符'),
  imageUrls: z
    .array(z.string())
    .min(1, '请至少上传一张图片')
    .max(9, '最多上传9张图片'),
});

export type PublishFormData = z.infer<typeof publishSchema>;
