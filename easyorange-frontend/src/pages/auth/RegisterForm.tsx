import { zodResolver } from '@hookform/resolvers/zod';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { Checkbox, Input, Label } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { useLogin, useRegister } from '@/hooks';
import { type RegisterForm as RegisterFormValues, registerSchema } from '@/schemas/authSchema';
import { useUIStore } from '@/store/uiStore';
import { errorHandler } from '@/utils/errorHandler';

interface RegisterFormProps {
    onRegisterSuccess: (username: string) => void;
    onSwitchToLogin: () => void;
}

export function RegisterForm({ onRegisterSuccess, onSwitchToLogin }: RegisterFormProps) {
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [capsLock, setCapsLock] = useState(false);
    const login = useLogin();
    const register = useRegister();
    const addToast = useUIStore(s => s.addToast);

    const registerForm = useForm<RegisterFormValues>({
        resolver: zodResolver(registerSchema),
        reValidateMode: 'onChange',
        defaultValues: { username: '', password: '', confirmPassword: '', agreeTerms: false },
    });
    const regVals = registerForm.watch();

    const toastFirstError = (errors: { [x: string]: { message?: string } | undefined }) => {
        const firstError = Object.values(errors).find(e => e?.message);
        if (firstError?.message) {
            addToast({ type: 'error', message: firstError.message });
        }
    };

    const handleCapsLock = (e: React.KeyboardEvent<HTMLInputElement>) => {
        try {
            setCapsLock(e.getModifierState('CapsLock'));
        } catch {
            // getModifierState 在部分测试环境中不可用，静默忽略
        }
    };

    const onRegisterSubmit = registerForm.handleSubmit(
        async data => {
            setError(null);

            setIsLoading(true);
            try {
                await register.mutateAsync({
                    username: data.username,
                    password: data.password,
                });

                addToast({ type: 'success', message: '注册成功！正在登录...' });

                await login.mutateAsync({
                    account: data.username,
                    password: data.password,
                    loginMethod: 'password',
                });

                onRegisterSuccess(data.username);
            } catch (err: unknown) {
                const errorMessage = errorHandler.handle(err as Error, 'unknown');
                setError(errorMessage);
            } finally {
                setIsLoading(false);
            }
        },
        (errors: { [x: string]: { message?: string } | undefined }) => toastFirstError(errors)
    );

    return (
        <form className="auth-page-form auth-form-entrance" onSubmit={onRegisterSubmit}>
            <div className="auth-page-header">
                <h3>
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        className="auth-heading-icon"
                        aria-hidden="true"
                    >
                        <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                        <circle cx="8.5" cy="7" r="4" />
                        <line x1="20" y1="8" x2="20" y2="14" />
                        <line x1="23" y1="11" x2="17" y2="11" />
                    </svg>
                    创建账户
                </h3>
                <p>加入我们,开启 AI 工程化之旅</p>
            </div>

            <div className="auth-page-input-group">
                <div className="relative flex items-center">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground pointer-events-none"
                        aria-hidden="true"
                    >
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                    <Input
                        type="text"
                        placeholder="用户名"
                        required
                        autoComplete="username"
                        aria-label="用户名"
                        data-testid="input-register-username"
                        autoFocus
                        className="pl-10"
                        {...registerForm.register('username')}
                    />
                </div>
                <div className="auth-page-input-hint">3-20位，仅支持字母、数字和下划线</div>
            </div>

            <div className="auth-page-input-group">
                <div className="relative flex items-center">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground pointer-events-none"
                        aria-hidden="true"
                    >
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    <Input
                        type={showPassword ? 'text' : 'password'}
                        placeholder="密码"
                        onKeyDown={handleCapsLock}
                        onKeyUp={handleCapsLock}
                        required
                        autoComplete="new-password"
                        aria-label="密码"
                        data-testid="input-register-password"
                        className="pl-10 pr-10"
                        {...registerForm.register('password')}
                    />
                    <Button
                        type="button"
                        variant="ghost"
                        size="icon"
                        className="absolute right-1 top-1/2 -translate-y-1/2 h-8 w-8"
                        onClick={() => setShowPassword(!showPassword)}
                        aria-label={showPassword ? '隐藏密码' : '显示密码'}
                        tabIndex={-1}
                    >
                        {showPassword ? (
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                width="20"
                                height="20"
                                aria-hidden="true"
                            >
                                <path d="M17.94 17.94A10.07 10.07 0 0 1 12 20c-7 0-11-8-11-8a18.45 18.45 0 0 1 5.06-5.94M9.9 4.24A9.12 9.12 0 0 1 12 4c7 0 11 8 11 8a18.5 18.5 0 0 1-2.16 3.19m-6.72-1.07a3 3 0 1 1-4.24-4.24" />
                                <line x1="1" y1="1" x2="23" y2="23" />
                            </svg>
                        ) : (
                            <svg
                                viewBox="0 0 24 24"
                                fill="none"
                                stroke="currentColor"
                                strokeWidth="2"
                                width="20"
                                height="20"
                                aria-hidden="true"
                            >
                                <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                                <circle cx="12" cy="12" r="3" />
                            </svg>
                        )}
                    </Button>
                </div>
                {capsLock && <div className="caps-lock-warning">⚠ 大写锁定已开启，请注意密码大小写</div>}
                <div className="auth-page-input-hint">6-20位，需包含大小写字母和数字</div>
            </div>

            <div className="auth-page-input-group">
                <div className="relative flex items-center">
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground pointer-events-none"
                        aria-hidden="true"
                    >
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    <Input
                        type="password"
                        placeholder="确认密码"
                        required
                        autoComplete="new-password"
                        aria-label="确认密码"
                        data-testid="input-register-confirm-password"
                        className="pl-10"
                        {...registerForm.register('confirmPassword')}
                    />
                </div>
            </div>

            <Label htmlFor="agree-terms" className="auth-page-checkbox-label auth-page-terms-checkbox font-normal">
                <Checkbox
                    id="agree-terms"
                    checked={regVals.agreeTerms}
                    onCheckedChange={checked => registerForm.setValue('agreeTerms', checked === true)}
                    required
                />
                <span>
                    我已阅读并同意
                    <a href="/terms" target="_blank" rel="noopener noreferrer">
                        服务条款
                    </a>
                    和
                    <a href="/privacy" target="_blank" rel="noopener noreferrer">
                        隐私政策
                    </a>
                </span>
            </Label>

            {error && (
                <div className="auth-page-error-message" data-testid="register-error">
                    {error}
                </div>
            )}

            <Button
                type="submit"
                className="auth-page-btn btn-primary-gradient w-full"
                isLoading={isLoading}
                loadingText="注册中..."
                disabled={isLoading}
                data-testid="btn-register-submit"
            >
                注 册
            </Button>

            <div className="auth-page-form-footer">
                <p>
                    已有账户？
                    <Button type="button" variant="link" className="auth-page-switch-link" onClick={onSwitchToLogin}>
                        立即登录
                    </Button>
                </p>
            </div>
        </form>
    );
}
