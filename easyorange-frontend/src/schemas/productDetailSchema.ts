import { z } from 'zod';

/** 商品详情页下单弹窗表单 */
export const orderFormSchema = z.object({
    phone: z
        .string()
        .min(1, '请输入手机号')
        .regex(/^1[3-9]\d{9}$/, '请输入正确的手机号'),
    remark: z.string().max(200, '备注不能超过200字'),
});

export type OrderFormData = z.infer<typeof orderFormSchema>;

/** 商品详情页评价弹窗表单 */
export const reviewSchema = z.object({
    rating: z.number().min(1, '请选择评分').max(5),
    content: z.string().min(1, '请填写评价内容').max(500, '评价内容不能超过500字'),
});

export type ReviewFormData = z.infer<typeof reviewSchema>;
