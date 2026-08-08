import { useCallback, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { ProfileSetupModal } from '@/components/profile/ProfileSetupModal';
import { Button } from '@/components/ui/button';
import { useUIStore } from '@/store/uiStore';
import { LoginForm } from './LoginForm';
import { RegisterForm } from './RegisterForm';
import './login.css';

function LoginPage() {
    const [activeTab, setActiveTab] = useState<'login' | 'register'>('login');
    const [showProfileSetup, setShowProfileSetup] = useState(false);
    const [registeredUsername, setRegisteredUsername] = useState('');
    const navigate = useNavigate();
    const addToast = useUIStore(s => s.addToast);

    const handleLoginSuccess = useCallback(() => {
        addToast({ type: 'success', message: '登录成功' });
        const redirect = new URLSearchParams(window.location.search).get('redirect') || '/';
        navigate(redirect, { replace: true });
    }, [addToast, navigate]);

    const handleRegisterSuccess = useCallback((username: string) => {
        setRegisteredUsername(username);
        setShowProfileSetup(true);
    }, []);

    return (
        <div className="auth-page-container">
            <Button
                variant="ghost"
                size="icon"
                className="auth-page-close-btn"
                onClick={() => navigate('/')}
                aria-label="关闭登录页"
            >
                <svg
                    viewBox="0 0 24 24"
                    fill="none"
                    stroke="currentColor"
                    strokeWidth="2"
                    strokeLinecap="round"
                    strokeLinejoin="round"
                    aria-hidden="true"
                >
                    <line x1="18" y1="6" x2="6" y2="18" />
                    <line x1="6" y1="6" x2="18" y2="18" />
                </svg>
            </Button>

            <div className="auth-page-bg">
                <div className="bg-gradient-mesh"></div>
                <div className="floating-orbs">
                    <div className="orb orb-1"></div>
                    <div className="orb orb-2"></div>
                    <div className="orb orb-3"></div>
                    <div className="orb orb-4"></div>
                    <div className="orb orb-5"></div>
                    <div className="orb orb-6"></div>
                    <div className="orb orb-7"></div>
                </div>
                <div className="aurora-bg"></div>
            </div>

            <div className="auth-page-modal">
                <div className="auth-page-brand-panel">
                    <div className="auth-page-brand-bg">
                        <div className="auth-page-brand-gradient"></div>
                        <div className="auth-page-brand-shapes">
                            <div className="auth-page-shape auth-page-shape--1"></div>
                            <div className="auth-page-shape auth-page-shape--2"></div>
                            <div className="auth-page-shape auth-page-shape--3"></div>
                            <div className="auth-page-shape auth-page-shape--4"></div>
                            <div className="auth-page-shape auth-page-shape--5"></div>
                        </div>
                    </div>
                    <div className="auth-page-brand-content">
                        <div className="auth-page-brand-logo">
                            <svg viewBox="0 0 48 48" fill="none" aria-hidden="true">
                                <defs>
                                    <linearGradient id="authLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                                        <stop offset="0%" stopColor="#F97316" />
                                        <stop offset="40%" stopColor="#FB7185" />
                                        <stop offset="100%" stopColor="#C39BD3" />
                                    </linearGradient>
                                </defs>
                                <path
                                    d="M8 12h13M8 12v24M8 36h13M8 12h8M8 24h10"
                                    stroke="url(#authLogoGradient)"
                                    strokeWidth="3"
                                    strokeLinecap="round"
                                    strokeLinejoin="round"
                                />
                                <circle cx="36" cy="24" r="10" stroke="url(#authLogoGradient)" strokeWidth="3" />
                            </svg>
                        </div>
                        <h2 className="auth-page-brand-title">EasyOrange</h2>
                        <p className="auth-page-brand-subtitle">AI 工程化</p>
                        <div className="auth-page-brand-features">
                            <div className="auth-page-brand-feature">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    aria-hidden="true"
                                >
                                    <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z" />
                                </svg>
                                <span>安全交易保障</span>
                            </div>
                            <div className="auth-page-brand-feature">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    aria-hidden="true"
                                >
                                    <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                                    <circle cx="9" cy="7" r="4" />
                                    <path d="M23 21v-2a4 4 0 0 0-3-3.87" />
                                    <path d="M16 3.13a4 4 0 0 1 0 7.75" />
                                </svg>
                                <span>AI 工程化</span>
                            </div>
                            <div className="auth-page-brand-feature">
                                <svg
                                    viewBox="0 0 24 24"
                                    fill="none"
                                    stroke="currentColor"
                                    strokeWidth="2"
                                    aria-hidden="true"
                                >
                                    <circle cx="12" cy="12" r="10" />
                                    <polyline points="12 6 12 12 16 14" />
                                </svg>
                                <span>快速便捷发布</span>
                            </div>
                        </div>
                    </div>
                </div>

                <div className="auth-page-form-panel">
                    <div className="auth-page-tabs">
                        <Button
                            type="button"
                            variant="ghost"
                            className={`auth-page-tab ${activeTab === 'login' ? 'auth-page-tab--active' : ''}`}
                            onClick={() => setActiveTab('login')}
                            data-testid="tab-login"
                        >
                            <span>登录</span>
                        </Button>
                        <Button
                            type="button"
                            variant="ghost"
                            className={`auth-page-tab ${activeTab === 'register' ? 'auth-page-tab--active' : ''}`}
                            onClick={() => setActiveTab('register')}
                            data-testid="tab-register"
                        >
                            <span>注册</span>
                        </Button>
                    </div>

                    {activeTab === 'login' && (
                        <LoginForm
                            onLoginSuccess={handleLoginSuccess}
                            onSwitchToRegister={() => setActiveTab('register')}
                        />
                    )}

                    {activeTab === 'register' && (
                        <RegisterForm
                            onRegisterSuccess={handleRegisterSuccess}
                            onSwitchToLogin={() => setActiveTab('login')}
                        />
                    )}
                </div>
            </div>

            <ProfileSetupModal
                isOpen={showProfileSetup}
                onClose={() => setShowProfileSetup(false)}
                username={registeredUsername}
            />
        </div>
    );
}

export default LoginPage;
