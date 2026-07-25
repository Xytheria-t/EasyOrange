import { z } from 'zod';

/** 密码登录表单 */
export const passwordLoginSchema = z.object({
    account: z.string().min(1, '请输入账号'),
    password: z.string().min(1, '请输入密码'),
});

/** 短信登录表单 */
export const smsLoginSchema = z.object({
    phone: z
        .string()
        .min(1, '请输入手机号')
        .regex(/^1[3-9]\d{9}$/, '请输入有效的手机号'),
    smsCode: z.string().min(1, '请输入验证码').max(6, '验证码最多6位'),
});

/** 注册表单 */
export const registerSchema = z
    .object({
        username: z
            .string()
            .min(3, '用户名至少需要3个字符')
            .max(20, '用户名不能超过20个字符')
            .regex(/^[a-zA-Z0-9_]+$/, '用户名只能包含字母、数字和下划线'),
        password: z.string().min(8, '密码至少需要8个字符').max(128, '密码不能超过128个字符'),
        confirmPassword: z.string().min(1, '请确认密码'),
        agreeTerms: z.literal(true, { message: '请同意服务条款和隐私政策' }),
    })
    .refine(data => data.password === data.confirmPassword, {
        message: '两次输入的密码不一致',
        path: ['confirmPassword'],
    });

/** 重置密码表单 */
export const resetPasswordSchema = z
    .object({
        phone: z
            .string()
            .min(1, '请输入手机号')
            .regex(/^1[3-9]\d{9}$/, '请输入有效的手机号'),
        verifyCode: z.string().min(1, '请输入验证码').max(6),
        newPassword: z.string().min(8, '密码至少需要8个字符').max(128, '密码不能超过128个字符'),
        confirmPassword: z.string().min(1, '请确认密码'),
    })
    .refine(data => data.newPassword === data.confirmPassword, {
        message: '两次输入的密码不一致',
        path: ['confirmPassword'],
    });

/** 修改密码表单 */
export const changePasswordSchema = z
    .object({
        oldPassword: z.string().min(1, '请输入旧密码'),
        newPassword: z.string().min(8, '密码至少需要8个字符').max(128, '密码不能超过128个字符'),
        confirmPassword: z.string().min(1, '请确认密码'),
    })
    .refine(data => data.newPassword === data.confirmPassword, {
        message: '两次输入的密码不一致',
        path: ['confirmPassword'],
    });

export type PasswordLoginForm = z.infer<typeof passwordLoginSchema>;
export type SmsLoginForm = z.infer<typeof smsLoginSchema>;
export type RegisterForm = z.infer<typeof registerSchema>;
export type ResetPasswordForm = z.infer<typeof resetPasswordSchema>;
export type ChangePasswordForm = z.infer<typeof changePasswordSchema>;
