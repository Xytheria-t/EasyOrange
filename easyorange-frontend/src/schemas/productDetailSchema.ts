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
