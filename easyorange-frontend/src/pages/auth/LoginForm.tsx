import { zodResolver } from '@hookform/resolvers/zod';
import { useEffect, useMemo, useRef, useState } from 'react';
import { type Resolver, useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api/userApi';
import { Checkbox, Input, Label } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { useLogin } from '@/hooks';
import { type PasswordLoginForm, passwordLoginSchema, type SmsLoginForm, smsLoginSchema } from '@/schemas/authSchema';
import { useUIStore } from '@/store/uiStore';
import { errorHandler } from '@/utils/errorHandler';

type LoginMethod = 'password' | 'sms';

type LoginFormValues = PasswordLoginForm & SmsLoginForm;

interface LoginFormProps {
    onLoginSuccess: () => void;
    onSwitchToRegister: () => void;
}

export function LoginForm({ onLoginSuccess, onSwitchToRegister }: LoginFormProps) {
    const [loginMethod, setLoginMethod] = useState<LoginMethod>('password');
    const [countdown, setCountdown] = useState(0);
    const [isSendingCode, setIsSendingCode] = useState(false);
    const [error, setError] = useState<string | null>(null);
    const [isLoading, setIsLoading] = useState(false);
    const [showPassword, setShowPassword] = useState(false);
    const [capsLock, setCapsLock] = useState(false);
    const [rememberMe, setRememberMe] = useState(false);
    const navigate = useNavigate();
    const login = useLogin();
    const addToast = useUIStore(s => s.addToast);
    const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null);

    const loginMethodRef = useRef(loginMethod);
    loginMethodRef.current = loginMethod;

    const loginForm = useForm<LoginFormValues>({
        resolver: useMemo(
            () => (values, ctx, opts) => {
                const schema = loginMethodRef.current === 'sms' ? smsLoginSchema : passwordLoginSchema;
                return (zodResolver(schema) as unknown as Resolver<LoginFormValues>)(values, ctx, opts);
            },
            []
        ),
        reValidateMode: 'onChange',
        defaultValues: { account: '', password: '', phone: '', smsCode: '' },
    });
    const loginVals = loginForm.watch();

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

    useEffect(() => {
        return () => {
            if (countdownRef.current) {
                clearInterval(countdownRef.current);
            }
        };
    }, []);

    const startCountdown = () => {
        setCountdown(60);
        countdownRef.current = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) {
                    if (countdownRef.current) {
                        clearInterval(countdownRef.current);
                    }
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    };

    const handleSendSmsCode = async () => {
        const valid = await loginForm.trigger('phone');
        if (!valid) {
            addToast({ type: 'error', message: loginForm.formState.errors.phone?.message ?? '请输入手机号' });
            return;
        }
        setIsSendingCode(true);
        try {
            await userApi.sendSmsCode(loginForm.getValues('phone'));
            startCountdown();
            addToast({ type: 'success', message: '验证码已发送' });
        } catch (err) {
            const msg = errorHandler.handle(err as Error, 'unknown');
            addToast({ type: 'error', message: msg });
        } finally {
            setIsSendingCode(false);
        }
    };

    const handleLoginMethodChange = (method: LoginMethod) => {
        setLoginMethod(method);
        loginForm.reset({ account: '', password: '', phone: '', smsCode: '' });
        setError(null);
    };

    const onLoginSubmit = loginForm.handleSubmit(
        async data => {
            setError(null);

            setIsLoading(true);
            try {
                await login.mutateAsync({
                    account: loginMethod === 'password' ? data.account : data.phone,
                    password: loginMethod === 'password' ? data.password : data.smsCode,
                    loginMethod,
                });
                onLoginSuccess();
            } catch (err) {
                const errorMessage = errorHandler.handle(err as Error);
                setError(errorMessage);
            } finally {
                setIsLoading(false);
            }
        },
        (errors: { [x: string]: { message?: string } | undefined }) => toastFirstError(errors)
    );

    return (
        <form className="auth-page-form auth-form-entrance" onSubmit={onLoginSubmit}>
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
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                        <circle cx="12" cy="7" r="4" />
                    </svg>
                    欢迎回来
                </h3>
                <p>登录账户，继续探索资产</p>
            </div>

            <div className="auth-page-login-method-toggle">
                <Button
                    type="button"
                    variant="outline"
                    className={`auth-page-method-btn ${loginMethod === 'password' ? 'auth-page-method-btn--active' : ''}`}
                    onClick={() => handleLoginMethodChange('password')}
                >
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        width="16"
                        height="16"
                        aria-hidden="true"
                    >
                        <rect x="3" y="11" width="18" height="11" rx="2" ry="2" />
                        <path d="M7 11V7a5 5 0 0 1 10 0v4" />
                    </svg>
                    密码登录
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    className={`auth-page-method-btn ${loginMethod === 'sms' ? 'auth-page-method-btn--active' : ''}`}
                    onClick={() => handleLoginMethodChange('sms')}
                >
                    <svg
                        viewBox="0 0 24 24"
                        fill="none"
                        stroke="currentColor"
                        strokeWidth="2"
                        width="16"
                        height="16"
                        aria-hidden="true"
                    >
                        <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
                        <line x1="12" y1="18" x2="12.01" y2="18" />
                    </svg>
                    短信登录
                </Button>
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
                        {loginMethod === 'password' ? (
                            <>
                                <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                                <circle cx="12" cy="7" r="4" />
                            </>
                        ) : (
                            <>
                                <rect x="5" y="2" width="14" height="20" rx="2" ry="2" />
                                <line x1="12" y1="18" x2="12.01" y2="18" />
                            </>
                        )}
                    </svg>
                    <Input
                        type={loginMethod === 'sms' ? 'tel' : 'text'}
                        placeholder={loginMethod === 'password' ? '用户名 / 邮箱 / 手机号' : '请输入手机号'}
                        required
                        autoComplete={loginMethod === 'sms' ? 'tel' : 'username'}
                        maxLength={loginMethod === 'sms' ? 11 : undefined}
                        aria-label={loginMethod === 'password' ? '用户名、邮箱或手机号' : '手机号'}
                        data-testid="input-account"
                        autoFocus
                        className="pl-10"
                        {...loginForm.register(loginMethod === 'sms' ? 'phone' : 'account')}
                    />
                </div>
            </div>

            {loginMethod === 'password' ? (
                <div className="auth-page-input-group auth-field-enter" key="login-password-field">
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
                            autoComplete="current-password"
                            aria-label="密码"
                            data-testid="input-password"
                            className="pl-10 pr-10"
                            {...loginForm.register('password')}
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
                </div>
            ) : (
                <div className="auth-page-input-group auth-field-enter" key="login-sms-field">
                    <div className="relative flex items-center">
                        <svg
                            viewBox="0 0 24 24"
                            fill="none"
                            stroke="currentColor"
                            strokeWidth="2"
                            className="absolute left-3 top-1/2 -translate-y-1/2 h-5 w-5 text-muted-foreground pointer-events-none"
                            aria-hidden="true"
                        >
                            <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                        </svg>
                        <Input
                            type="text"
                            placeholder="请输入验证码"
                            required
                            maxLength={6}
                            autoComplete="one-time-code"
                            aria-label="短信验证码"
                            className="pl-10 pr-24"
                            {...loginForm.register('smsCode')}
                        />
                        <Button
                            type="button"
                            size="sm"
                            className="absolute right-2 top-1/2 -translate-y-1/2"
                            onClick={handleSendSmsCode}
                            disabled={countdown > 0 || isSendingCode || !loginVals.phone}
                        >
                            {isSendingCode ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码'}
                        </Button>
                    </div>
                </div>
            )}

            <div className="auth-page-form-options">
                {loginMethod === 'password' ? (
                    <>
                        <Label htmlFor="remember-me" className="auth-page-checkbox-label font-normal">
                            <Checkbox
                                id="remember-me"
                                checked={rememberMe}
                                onCheckedChange={checked => setRememberMe(checked === true)}
                            />
                            <span>记住我</span>
                        </Label>
                        <Button
                            type="button"
                            variant="link"
                            className="auth-page-forgot-link"
                            onClick={() => navigate('/forgot-password')}
                            data-testid="link-forgot-password"
                        >
                            忘记密码？
                        </Button>
                    </>
                ) : (
                    <Button
                        type="button"
                        variant="link"
                        className="auth-page-forgot-link"
                        onClick={() => handleLoginMethodChange('password')}
                    >
                        使用密码登录
                    </Button>
                )}
            </div>

            {error && (
                <div className="auth-page-error-message" data-testid="login-error">
                    {error}
                </div>
            )}

            <Button
                type="submit"
                className="auth-page-btn btn-primary-gradient w-full"
                isLoading={isLoading}
                loadingText="登录中..."
                disabled={isLoading}
                data-testid="btn-login-submit"
            >
                登 录
            </Button>

            <div className="auth-page-divider">
                <span>或</span>
            </div>

            <div className="auth-page-social-login">
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="auth-page-social-btn social-btn--wechat"
                    aria-label="微信登录"
                    onClick={() => addToast({ type: 'info', message: '微信登录功能开发中，敬请期待' })}
                >
                    <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true">
                        <path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.269-.03-.406-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z" />
                    </svg>
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="auth-page-social-btn social-btn--qq"
                    aria-label="QQ 登录"
                    onClick={() => addToast({ type: 'info', message: 'QQ登录功能开发中，敬请期待' })}
                >
                    <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true">
                        <path d="M12.003 2c-2.265 0-6.29 1.364-6.29 7.325v1.195S3.55 14.96 3.55 17.474c0 .665.17 1.025.281 1.025.114 0 .902-.484 1.748-2.072 0 0-.18 2.197 1.904 3.967 0 0-1.77.495-1.77 1.182 0 .686 1.865 1.152 4.063 1.152 2.197 0 4.062-.466 4.062-1.152 0-.687-1.77-1.182-1.77-1.182 2.085-1.77 1.905-3.967 1.905-3.967.845 1.588 1.634 2.072 1.746 2.072.111 0 .283-.36.283-1.025 0-2.514-2.166-6.954-2.166-6.954V9.325C18.29 3.364 14.268 2 12.003 2z" />
                    </svg>
                </Button>
                <Button
                    type="button"
                    variant="outline"
                    size="icon"
                    className="auth-page-social-btn social-btn--weibo"
                    aria-label="微博登录"
                    onClick={() => addToast({ type: 'info', message: '微博登录功能开发中，敬请期待' })}
                >
                    <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20" aria-hidden="true">
                        <path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.601.622.263.82.972.442 1.592zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.09-.313-.361-.177-.586.138-.227.436-.346.672-.24.239.09.315.36.194.573zm.176-2.719c-1.893-.493-4.033.45-4.857 2.118-.836 1.704-.026 3.591 1.886 4.21 1.983.64 4.318-.341 5.132-2.179.8-1.793-.201-3.642-2.161-4.149zm7.563-1.224c-.346-.105-.579-.18-.405-.649.381-1.017.422-1.896-.006-2.523-.801-1.169-2.992-1.107-5.528-.03 0 0-.792.346-.589-.283.389-1.229.332-2.258-.276-2.851-1.379-1.345-5.049.051-8.199 3.118C.964 11.652 0 14.31 0 16.552c0 4.283 5.503 6.893 10.89 6.893 7.065 0 11.771-4.104 11.771-7.361 0-1.967-1.66-3.083-3.602-3.435z" />
                    </svg>
                </Button>
            </div>

            <div className="auth-page-form-footer">
                <p>
                    还没有账户？
                    <Button type="button" variant="link" className="auth-page-switch-link" onClick={onSwitchToRegister}>
                        立即注册
                    </Button>
                </p>
            </div>
        </form>
    );
}
