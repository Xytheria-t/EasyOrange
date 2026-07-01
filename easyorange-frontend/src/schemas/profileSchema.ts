import { z } from 'zod';

/** 完善个人信息表单 */
export const profileSetupSchema = z.object({
    realName: z
        .string()
        .min(2, '真实姓名至少需要2个字符')
        .max(10, '真实姓名不能超过10个字符')
        .regex(/^[\u4e00-\u9fa5a-zA-Z\s]+$/, '姓名只能包含中文、英文字母和空格'),
    studentId: z.string().min(5, '请输入有效的学号'),
    email: z.string().min(1, '邮箱不能为空').email('请输入有效的邮箱地址'),
    phone: z
        .string()
        .min(1, '手机号不能为空')
        .regex(/^1[3-9]\d{9}$/, '请输入有效的11位手机号'),
});

export type ProfileSetupForm = z.infer<typeof profileSetupSchema>;
