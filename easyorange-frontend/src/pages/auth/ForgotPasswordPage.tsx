import { zodResolver } from '@hookform/resolvers/zod';
import { ArrowLeft, Check, KeyRound, ShieldCheck, Smartphone, Sparkles } from 'lucide-react';
import { useState } from 'react';
import { useForm } from 'react-hook-form';
import { useNavigate } from 'react-router-dom';
import { userApi } from '@/api/userApi';
import { Input, Label } from '@/components/ui';
import { Button } from '@/components/ui/button';
import { type ResetPasswordForm, resetPasswordSchema } from '@/schemas/authSchema';
import { useUIStore } from '@/store/uiStore';
import { errorHandler } from '@/utils/errorHandler';
import './forgot-password.css';

type Step = 1 | 2 | 3;

function ForgotPasswordPage() {
    const navigate = useNavigate();
    const addToast = useUIStore(s => s.addToast);
    const [step, setStep] = useState<Step>(1);
    const [isLoading, setIsLoading] = useState(false);
    const [countdown, setCountdown] = useState(0);

    const { register, watch, formState, trigger } = useForm<ResetPasswordForm>({
        resolver: zodResolver(resetPasswordSchema),
        reValidateMode: 'onChange',
        defaultValues: { phone: '', verifyCode: '', newPassword: '', confirmPassword: '' },
    });
    const vals = watch();

    const startCountdown = () => {
        setCountdown(60);
        const timer = setInterval(() => {
            setCountdown(prev => {
                if (prev <= 1) {
                    clearInterval(timer);
                    return 0;
                }
                return prev - 1;
            });
        }, 1000);
    };

    const handleSendCode = async () => {
        const valid = await trigger('phone');
        if (!valid) {
            addToast({ type: 'warning', message: formState.errors.phone?.message || '请输入手机号' });
            return;
        }
        try {
            await userApi.sendSmsCode(vals.phone);
            startCountdown();
            setStep(2);
            addToast({ type: 'success', message: '验证码已发送' });
        } catch (err) {
            const msg = errorHandler.handle(err as Error);
            addToast({ type: 'error', message: msg });
        }
    };

    const handleVerifyCode = async () => {
        const valid = await trigger('verifyCode');
        if (!valid) {
            addToast({ type: 'warning', message: formState.errors.verifyCode?.message || '请输入验证码' });
            return;
        }
        setStep(3);
    };

    const handleResetPassword = async () => {
        if (!vals.newPassword) {
            addToast({ type: 'warning', message: '请输入新密码' });
            return;
        }
        const valid = await trigger(['newPassword', 'confirmPassword']);
        if (!valid) {
            const errMsg =
                formState.errors.confirmPassword?.message || formState.errors.newPassword?.message || '密码不符合要求';
            addToast({ type: 'warning', message: errMsg });
            return;
        }
        setIsLoading(true);
        try {
            await userApi.forgotPassword({
                phone: vals.phone,
                verifyCode: vals.verifyCode,
                newPassword: vals.newPassword,
            });
            addToast({ type: 'success', message: '密码重置成功，请使用新密码登录' });
            navigate('/login');
        } catch (err) {
            const msg = errorHandler.handle(err as Error);
            addToast({ type: 'error', message: msg });
        } finally {
            setIsLoading(false);
        }
    };

    const stepConfig = [
        { num: 1, label: '验证手机', icon: Smartphone, desc: '输入注册手机号' },
        { num: 2, label: '输入验证码', icon: ShieldCheck, desc: '验证身份' },
        { num: 3, label: '重置密码', icon: KeyRound, desc: '设置新密码' },
    ];

    return (
        <div className="forgot-password-page">
            <div className="forgot-password-bg">
                <div className="bg-gradient-mesh"></div>
                <div className="floating-orbs">
                    <div className="orb orb-1"></div>
                    <div className="orb orb-2"></div>
                    <div className="orb orb-3"></div>
                </div>
            </div>

            <Button
                variant="ghost"
                size="icon"
                className="forgot-password-close-btn"
                onClick={() => navigate('/login')}
                aria-label="返回登录"
            >
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
            </Button>

            <div className="forgot-password-container">
                <div className="forgot-password-card">
                    <div className="forgot-password-header">
                        <div className="forgot-password-icon">
                            <KeyRound size={24} />
                        </div>
                        <h1 className="forgot-password-title">忘记密码</h1>
                        <p className="forgot-password-subtitle">通过手机验证码重置您的密码</p>
                    </div>

                    <div className="forgot-password-steps">
                        {stepConfig.map(({ num, label, icon: Icon, desc }, idx) => (
                            <div key={num} className="forgot-password-step">
                                <div
                                    className={`forgot-password-step-indicator ${step >= num ? 'active' : ''} ${step > num ? 'completed' : ''}`}
                                >
                                    {step > num ? <Check size={14} /> : <Icon size={14} />}
                                </div>
                                <div className="forgot-password-step-content">
                                    <span className="forgot-password-step-label">{label}</span>
                                    <span className="forgot-password-step-desc">{desc}</span>
                                </div>
                                {idx < stepConfig.length - 1 && (
                                    <div className={`forgot-password-step-line ${step > num ? 'completed' : ''}`}></div>
                                )}
                            </div>
                        ))}
                    </div>

                    <div className="forgot-password-form">
                        {step === 1 && (
                            <div className="forgot-password-form-step">
                                <div className="form-group">
                                    <Label htmlFor="forgot-phone">手机号</Label>
                                    <div className="relative flex items-center">
                                        <Smartphone size={18} className="input-icon" />
                                        <Input
                                            id="forgot-phone"
                                            type="tel"
                                            placeholder="请输入注册时绑定的手机号"
                                            maxLength={11}
                                            data-testid="input-forgot-phone"
                                            className="pl-11"
                                            {...register('phone')}
                                        />
                                    </div>
                                </div>
                                <Button
                                    className="forgot-password-btn"
                                    onClick={handleSendCode}
                                    disabled={countdown > 0}
                                    data-testid="btn-send-code"
                                >
                                    <Sparkles size={16} />
                                    {countdown > 0 ? `${countdown}s 后重新发送` : '发送验证码'}
                                </Button>
                            </div>
                        )}

                        {step === 2 && (
                            <div className="forgot-password-form-step">
                                <div className="form-group">
                                    <Label htmlFor="forgot-verify-code">验证码</Label>
                                    <div className="relative flex items-center">
                                        <ShieldCheck size={18} className="input-icon" />
                                        <Input
                                            id="forgot-verify-code"
                                            type="text"
                                            placeholder="请输入6位验证码"
                                            maxLength={6}
                                            data-testid="input-verify-code"
                                            className="pl-11 pr-20"
                                            {...register('verifyCode')}
                                        />
                                        <Button
                                            variant="ghost"
                                            size="sm"
                                            onClick={handleSendCode}
                                            disabled={countdown > 0}
                                            className="absolute right-2 top-1/2 -translate-y-1/2 h-7 px-2 text-xs font-semibold text-primary-600"
                                        >
                                            {countdown > 0 ? `${countdown}s` : '重新发送'}
                                        </Button>
                                    </div>
                                </div>
                                <Button
                                    className="forgot-password-btn"
                                    onClick={handleVerifyCode}
                                    data-testid="btn-verify-next"
                                >
                                    下一步
                                </Button>
                            </div>
                        )}

                        {step === 3 && (
                            <div className="forgot-password-form-step">
                                <div className="form-group">
                                    <Label htmlFor="forgot-new-password">新密码</Label>
                                    <div className="relative flex items-center">
                                        <KeyRound size={18} className="input-icon" />
                                        <Input
                                            id="forgot-new-password"
                                            type="password"
                                            placeholder="需包含大小写字母和数字，6-20位"
                                            data-testid="input-new-password"
                                            className="pl-11"
                                            {...register('newPassword')}
                                        />
                                    </div>
                                </div>
                                <div className="form-group">
                                    <Label htmlFor="forgot-confirm-password">确认新密码</Label>
                                    <div className="relative flex items-center">
                                        <KeyRound size={18} className="input-icon" />
                                        <Input
                                            id="forgot-confirm-password"
                                            type="password"
                                            placeholder="再次输入新密码"
                                            data-testid="input-confirm-new-password"
                                            className="pl-11"
                                            {...register('confirmPassword')}
                                        />
                                    </div>
                                </div>
                                <Button
                                    className="forgot-password-btn"
                                    onClick={handleResetPassword}
                                    disabled={isLoading}
                                    data-testid="btn-reset-password"
                                >
                                    {isLoading ? '重置中...' : '重置密码'}
                                </Button>
                            </div>
                        )}
                    </div>

                    <div className="forgot-password-footer">
                        <Button variant="outline" className="back-to-login-btn" onClick={() => navigate('/login')}>
                            <ArrowLeft size={16} />
                            返回登录
                        </Button>
                    </div>
                </div>
            </div>
        </div>
    );
}

export default ForgotPasswordPage;
