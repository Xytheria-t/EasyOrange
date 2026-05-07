import { useState, useEffect, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useLogin, useRegister } from '@/hooks'
import { userApi } from '@/api/userApi'
import { validator, errorHandler } from '@/utils'
import { useUIStore } from '@/store/uiStore'
import { ProfileSetupModal } from '@/components/ProfileSetupModal'
import '@/styles/login.css'

type LoginMethod = 'password' | 'sms'

export function LoginPage() {
  const [activeTab, setActiveTab] = useState<'login' | 'register'>('login')
  const [loginMethod, setLoginMethod] = useState<LoginMethod>('password')
  const [formData, setFormData] = useState({
    account: '',
    password: '',
    confirmPassword: '',
    agreeTerms: false,
  })
  const [smsCode, setSmsCode] = useState('')
  const [countdown, setCountdown] = useState(0)
  const [isSendingCode, setIsSendingCode] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [isLoading, setIsLoading] = useState(false)
  const [showProfileSetup, setShowProfileSetup] = useState(false)
  const [registeredUsername, setRegisteredUsername] = useState('')
  const navigate = useNavigate()
  const login = useLogin()
  const register = useRegister()
  const addToast = useUIStore((s) => s.addToast)
  const countdownRef = useRef<ReturnType<typeof setInterval> | null>(null)

  const getLoginRedirect = () => {
    const params = new URLSearchParams(window.location.search)
    return params.get('redirect') || '/'
  }

  useEffect(() => {
    return () => {
      if (countdownRef.current) { clearInterval(countdownRef.current) }
    }
  }, [])

  const startCountdown = () => {
    setCountdown(60)
    countdownRef.current = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          if (countdownRef.current) { clearInterval(countdownRef.current) }
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const handleSendSmsCode = async () => {
    const phoneError = validator.getErrorMessage('phone', formData.account)
    if (phoneError) {
      addToast({ type: 'error', message: phoneError })
      return
    }
    setIsSendingCode(true)
    try {
      await userApi.sendSmsCode(formData.account)
      startCountdown()
      addToast({ type: 'success', message: '验证码已发送' })
    } catch (err) {
      const msg = errorHandler.handle(err as Error, 'unknown')
      addToast({ type: 'error', message: msg })
    } finally {
      setIsSendingCode(false)
    }
  }

  const handleLoginMethodChange = (method: LoginMethod) => {
    setLoginMethod(method)
    setFormData((prev) => ({ ...prev, account: '', password: '' }))
    setSmsCode('')
    setError(null)
  }

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError(null)

    if (activeTab === 'login') {
      if (loginMethod === 'password') {
        const usernameError = validator.getErrorMessage('username', formData.account)
        if (usernameError) {
          addToast({ type: 'error', message: usernameError })
          return
        }

        const passwordError = validator.getErrorMessage('password', formData.password)
        if (passwordError) {
          addToast({ type: 'error', message: passwordError })
          return
        }

        setIsLoading(true)
        try {
          await login.mutateAsync({
            account: formData.account,
            password: formData.password,
          })
          addToast({ type: 'success', message: '登录成功' })
          navigate(getLoginRedirect(), { replace: true })
        } catch (err) {
          const errorMessage = errorHandler.handle(err as Error)
          setError(errorMessage)
        } finally {
          setIsLoading(false)
        }
      } else {
        const phoneError = validator.getErrorMessage('phone', formData.account)
        if (phoneError) {
          addToast({ type: 'error', message: phoneError })
          return
        }

        if (!smsCode) {
          addToast({ type: 'error', message: '请输入验证码' })
          return
        }

        setIsLoading(true)
        try {
          await login.mutateAsync({
            account: formData.account,
            password: smsCode,
            loginMethod: 'sms',
          })
          addToast({ type: 'success', message: '登录成功' })
          navigate(getLoginRedirect(), { replace: true })
        } catch (err) {
          const errorMessage = errorHandler.handle(err as Error)
          setError(errorMessage)
        } finally {
          setIsLoading(false)
        }
      }
    } else {
      if (!formData.account || !formData.password || !formData.confirmPassword) {
        addToast({ type: 'error', message: '请填写完整信息' })
        return
      }

      if (!formData.agreeTerms) {
        addToast({ type: 'error', message: '请同意服务条款和隐私政策' })
        return
      }

      if (formData.password !== formData.confirmPassword) {
        addToast({ type: 'error', message: '两次输入的密码不一致' })
        return
      }

      const usernameError = validator.getErrorMessage('username', formData.account)
      if (usernameError) {
        addToast({ type: 'error', message: usernameError })
        return
      }

      const passwordError = validator.getErrorMessage('password', formData.password)
      if (passwordError) {
        addToast({ type: 'error', message: passwordError })
        return
      }

      setIsLoading(true)
      try {
        await register.mutateAsync({
          username: formData.account,
          password: formData.password
        })

        addToast({ type: 'success', message: '注册成功！正在登录...' })

        await login.mutateAsync({
          account: formData.account,
          password: formData.password
        })

        setRegisteredUsername(formData.account)
        setShowProfileSetup(true)
      } catch (err: unknown) {
        const errorMessage = errorHandler.handle(err as Error, 'unknown')
        setError(errorMessage)
      } finally {
        setIsLoading(false)
      }
    }
  }

  return (
    <div className="auth-page-container">
      <button className="auth-page-close-btn" onClick={() => navigate('/')} aria-label="关闭登录页">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>

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
              <svg viewBox="0 0 48 48" fill="none">
                <defs>
                  <linearGradient id="authLogoGradient" x1="0%" y1="0%" x2="100%" y2="100%">
                    <stop offset="0%" stopColor="#F97316"/>
                    <stop offset="40%" stopColor="#FB7185"/>
                    <stop offset="100%" stopColor="#C39BD3"/>
                  </linearGradient>
                </defs>
                <path d="M8 12h13M8 12v24M8 36h13M8 12h8M8 24h10" stroke="url(#authLogoGradient)" strokeWidth="3" strokeLinecap="round" strokeLinejoin="round"/>
                <circle cx="36" cy="24" r="10" stroke="url(#authLogoGradient)" strokeWidth="3"/>
              </svg>
            </div>
            <h2 className="auth-page-brand-title">EasyOrange</h2>
            <p className="auth-page-brand-subtitle">易橙坊</p>
            <div className="auth-page-brand-features">
              <div className="auth-page-brand-feature">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                </svg>
                <span>安全交易保障</span>
              </div>
              <div className="auth-page-brand-feature">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"/>
                  <circle cx="9" cy="7" r="4"/>
                  <path d="M23 21v-2a4 4 0 0 0-3-3.87"/>
                  <path d="M16 3.13a4 4 0 0 1 0 7.75"/>
                </svg>
                <span>校园专属社区</span>
              </div>
              <div className="auth-page-brand-feature">
                <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2">
                  <circle cx="12" cy="12" r="10"/>
                  <polyline points="12 6 12 12 16 14"/>
                </svg>
                <span>快速便捷发布</span>
              </div>
            </div>
          </div>
        </div>

        <div className="auth-page-form-panel">
          <div className="auth-page-tabs">
            <button
              type="button"
              className={`auth-page-tab ${activeTab === 'login' ? 'auth-page-tab--active' : ''}`}
              onClick={() => setActiveTab('login')}
              data-testid="tab-login"
            >
              <span>登录</span>
            </button>
            <button
              type="button"
              className={`auth-page-tab ${activeTab === 'register' ? 'auth-page-tab--active' : ''}`}
              onClick={() => setActiveTab('register')}
              data-testid="tab-register"
            >
              <span>注册</span>
            </button>
          </div>

          {activeTab === 'login' && (
            <form className="auth-page-form" onSubmit={handleSubmit}>
              <div className="auth-page-header">
                <h3>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-heading-icon">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2" />
                    <circle cx="12" cy="7" r="4" />
                  </svg>
                  欢迎回来
                </h3>
                <p>登录账户，继续探索好物</p>
              </div>

              <div className="auth-page-login-method-toggle">
                <button
                  type="button"
                  className={`auth-page-method-btn ${loginMethod === 'password' ? 'auth-page-method-btn--active' : ''}`}
                  onClick={() => handleLoginMethodChange('password')}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                  </svg>
                  密码登录
                </button>
                <button
                  type="button"
                  className={`auth-page-method-btn ${loginMethod === 'sms' ? 'auth-page-method-btn--active' : ''}`}
                  onClick={() => handleLoginMethodChange('sms')}
                >
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" width="16" height="16">
                    <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                    <line x1="12" y1="18" x2="12.01" y2="18"/>
                  </svg>
                  短信登录
                </button>
              </div>

              <div className="auth-page-input-group">
                <div className="auth-page-input-wrapper glass-input">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                    {loginMethod === 'password' ? (
                      <>
                        <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                        <circle cx="12" cy="7" r="4"/>
                      </>
                    ) : (
                      <>
                        <rect x="5" y="2" width="14" height="20" rx="2" ry="2"/>
                        <line x1="12" y1="18" x2="12.01" y2="18"/>
                      </>
                    )}
                  </svg>
                  <input
                    type={loginMethod === 'sms' ? 'tel' : 'text'}
                    placeholder={loginMethod === 'password' ? '用户名 / 邮箱 / 手机号' : '请输入手机号'}
                    value={formData.account}
                    onChange={(e) => setFormData((prev) => ({ ...prev, account: e.target.value }))}
                    required
                    autoComplete={loginMethod === 'sms' ? 'tel' : 'username'}
                    maxLength={loginMethod === 'sms' ? 11 : undefined}
                    data-testid="input-account"
                  />
                </div>
              </div>

              {loginMethod === 'password' ? (
                <div className="auth-page-input-group">
                  <div className="auth-page-input-wrapper glass-input">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                      <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                      <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                    </svg>
                    <input
                      type="password"
                      placeholder="密码"
                      value={formData.password}
                      onChange={(e) => setFormData((prev) => ({ ...prev, password: e.target.value }))}
                      required
                      autoComplete="current-password"
                      data-testid="input-password"
                    />
                  </div>
                </div>
              ) : (
                <div className="auth-page-input-group">
                  <div className="auth-page-input-wrapper glass-input">
                    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                      <path d="M12 22s8-4 8-10V5l-8-3-8 3v7c0 6 8 10 8 10z"/>
                    </svg>
                    <input
                      type="text"
                      placeholder="请输入验证码"
                      value={smsCode}
                      onChange={(e) => setSmsCode(e.target.value)}
                      required
                      maxLength={6}
                      autoComplete="one-time-code"
                    />
                    <button
                      type="button"
                      className="auth-page-sms-btn"
                      onClick={handleSendSmsCode}
                      disabled={countdown > 0 || isSendingCode || !formData.account}
                    >
                      {isSendingCode ? '发送中...' : countdown > 0 ? `${countdown}s` : '获取验证码'}
                    </button>
                  </div>
                </div>
              )}

              <div className="auth-page-form-options">
                {loginMethod === 'password' ? (
                  <>
                    <label className="auth-page-checkbox-label">
                      <input type="checkbox" />
                      <span className="auth-page-checkbox-custom"></span>
                      <span>记住我</span>
                    </label>
                    <button type="button" className="auth-page-forgot-link" onClick={() => navigate('/forgot-password')} data-testid="link-forgot-password">忘记密码？</button>
                  </>
                ) : (
                  <button type="button" className="auth-page-forgot-link" onClick={() => handleLoginMethodChange('password')}>使用密码登录</button>
                )}
              </div>

              {error && activeTab === 'login' && (
                <div className="auth-page-error-message" data-testid="login-error">
                  {error}
                </div>
              )}

              <button
                type="submit"
                className="auth-page-btn btn-primary-gradient"
                disabled={isLoading}
                data-testid="btn-login-submit"
              >
                <span className="btn-text">{isLoading ? '登录中...' : '登 录'}</span>
                {isLoading && <span className="btn-loader"><span className="loader"></span></span>}
              </button>

              <div className="auth-page-divider">
                <span>或</span>
              </div>

              <div className="auth-page-social-login">
                <button type="button" className="auth-page-social-btn social-btn--wechat" aria-label="微信登录">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M8.691 2.188C3.891 2.188 0 5.476 0 9.53c0 2.212 1.17 4.203 3.002 5.55a.59.59 0 0 1 .213.665l-.39 1.48c-.019.07-.048.141-.048.213 0 .163.13.295.29.295a.326.326 0 0 0 .167-.054l1.903-1.114a.864.864 0 0 1 .717-.098 10.16 10.16 0 0 0 2.837.403c.276 0 .543-.027.811-.05-.857-2.578.157-4.972 1.932-6.446 1.703-1.415 3.882-1.98 5.853-1.838-.576-3.583-4.196-6.348-8.596-6.348zM5.785 5.991c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178A1.17 1.17 0 0 1 4.623 7.17c0-.651.52-1.18 1.162-1.18zm5.813 0c.642 0 1.162.529 1.162 1.18a1.17 1.17 0 0 1-1.162 1.178 1.17 1.17 0 0 1-1.162-1.178c0-.651.52-1.18 1.162-1.18zm5.34 2.867c-1.797-.052-3.746.512-5.28 1.786-1.72 1.428-2.687 3.72-1.78 6.22.942 2.453 3.666 4.229 6.884 4.229.826 0 1.622-.12 2.361-.336a.722.722 0 0 1 .598.082l1.584.926a.272.272 0 0 0 .14.047c.134 0 .24-.111.24-.247 0-.06-.023-.12-.038-.177l-.327-1.233a.582.582 0 0 1-.023-.156.49.49 0 0 1 .201-.398C23.024 18.48 24 16.82 24 14.98c0-3.21-2.931-5.837-6.656-6.088V8.89c-.135-.01-.269-.03-.406-.03zm-2.53 3.274c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.97-.982zm4.844 0c.535 0 .969.44.969.982a.976.976 0 0 1-.969.983.976.976 0 0 1-.969-.983c0-.542.434-.982.969-.982z"/></svg>
                </button>
                <button type="button" className="auth-page-social-btn social-btn--qq" aria-label="QQ 登录">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M12.003 2c-2.265 0-6.29 1.364-6.29 7.325v1.195S3.55 14.96 3.55 17.474c0 .665.17 1.025.281 1.025.114 0 .902-.484 1.748-2.072 0 0-.18 2.197 1.904 3.967 0 0-1.77.495-1.77 1.182 0 .686 1.865 1.152 4.063 1.152 2.197 0 4.062-.466 4.062-1.152 0-.687-1.77-1.182-1.77-1.182 2.085-1.77 1.905-3.967 1.905-3.967.845 1.588 1.634 2.072 1.746 2.072.111 0 .283-.36.283-1.025 0-2.514-2.166-6.954-2.166-6.954V9.325C18.29 3.364 14.268 2 12.003 2z"/></svg>
                </button>
                <button type="button" className="auth-page-social-btn social-btn--weibo" aria-label="微博登录">
                  <svg viewBox="0 0 24 24" fill="currentColor" width="20" height="20"><path d="M10.098 20.323c-3.977.391-7.414-1.406-7.672-4.02-.259-2.609 2.759-5.047 6.74-5.441 3.979-.394 7.413 1.404 7.671 4.018.259 2.6-2.759 5.049-6.739 5.443zM9.05 17.219c-.384.616-1.208.884-1.829.602-.612-.279-.793-.991-.406-1.593.379-.595 1.176-.861 1.793-.601.622.263.82.972.442 1.592zm1.27-1.627c-.141.237-.449.353-.689.253-.236-.09-.313-.361-.177-.586.138-.227.436-.346.672-.24.239.09.315.36.194.573zm.176-2.719c-1.893-.493-4.033.45-4.857 2.118-.836 1.704-.026 3.591 1.886 4.21 1.983.64 4.318-.341 5.132-2.179.8-1.793-.201-3.642-2.161-4.149zm7.563-1.224c-.346-.105-.579-.18-.405-.649.381-1.017.422-1.896-.006-2.523-.801-1.169-2.992-1.107-5.528-.03 0 0-.792.346-.589-.283.389-1.229.332-2.258-.276-2.851-1.379-1.345-5.049.051-8.199 3.118C.964 11.652 0 14.31 0 16.552c0 4.283 5.503 6.893 10.89 6.893 7.065 0 11.771-4.104 11.771-7.361 0-1.967-1.66-3.083-3.602-3.435z"/></svg>
                </button>
              </div>

              <div className="auth-page-form-footer">
                <p>还没有账户？<button type="button" className="auth-page-switch-link" onClick={() => setActiveTab('register')}>立即注册</button></p>
              </div>
            </form>
          )}

          {activeTab === 'register' && (
            <form className="auth-page-form" onSubmit={handleSubmit}>
              <div className="auth-page-header">
                <h3>
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-heading-icon">
                    <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" />
                    <circle cx="8.5" cy="7" r="4" />
                    <line x1="20" y1="8" x2="20" y2="14" />
                    <line x1="23" y1="11" x2="17" y2="11" />
                  </svg>
                  创建账户
                </h3>
                <p>加入我们，开始校园交易之旅</p>
              </div>

              <div className="auth-page-input-group">
                <div className="auth-page-input-wrapper glass-input">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                    <path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"/>
                    <circle cx="12" cy="7" r="4"/>
                  </svg>
                  <input
                    type="text"
                    placeholder="用户名"
                    value={formData.account}
                    onChange={(e) => setFormData((prev) => ({ ...prev, account: e.target.value }))}
                    required
                    autoComplete="username"
                    data-testid="input-register-username"
                  />
                </div>
                <div className="auth-page-input-hint">3-20位，仅支持字母、数字和下划线</div>
              </div>

              <div className="auth-page-input-group">
                <div className="auth-page-input-wrapper glass-input">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                  </svg>
                  <input
                    type="password"
                    placeholder="密码"
                    value={formData.password}
                    onChange={(e) => setFormData((prev) => ({ ...prev, password: e.target.value }))}
                    required
                    autoComplete="new-password"
                    data-testid="input-register-password"
                  />
                </div>
                <div className="auth-page-input-hint">6-20位，需包含大小写字母和数字</div>
              </div>

              <div className="auth-page-input-group">
                <div className="auth-page-input-wrapper glass-input">
                  <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" className="auth-page-input-icon">
                    <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
                    <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
                  </svg>
                  <input
                    type="password"
                    placeholder="确认密码"
                    value={formData.confirmPassword}
                    onChange={(e) => setFormData((prev) => ({ ...prev, confirmPassword: e.target.value }))}
                    required
                    autoComplete="new-password"
                    data-testid="input-register-confirm-password"
                  />
                </div>
              </div>

              <label className="auth-page-checkbox-label auth-page-terms-checkbox">
                <input
                  type="checkbox"
                  checked={formData.agreeTerms}
                  onChange={(e) => setFormData((prev) => ({ ...prev, agreeTerms: e.target.checked }))}
                  required
                />
                <span className="auth-page-checkbox-custom"></span>
                <span>我已阅读并同意<a href="#">服务条款</a>和<a href="#">隐私政策</a></span>
              </label>

              {error && activeTab === 'register' && (
                <div className="auth-page-error-message" data-testid="register-error">
                  {error}
                </div>
              )}

              <button
                type="submit"
                className="auth-page-btn btn-primary-gradient"
                disabled={isLoading}
                data-testid="btn-register-submit"
              >
                <span className="btn-text">{isLoading ? '注册中...' : '注 册'}</span>
                {isLoading && <span className="btn-loader"><span className="loader"></span></span>}
              </button>

              <div className="auth-page-form-footer">
                <p>已有账户？<button type="button" className="auth-page-switch-link" onClick={() => setActiveTab('login')}>立即登录</button></p>
              </div>
            </form>
          )}
        </div>
      </div>

      <ProfileSetupModal
        isOpen={showProfileSetup}
        onClose={() => setShowProfileSetup(false)}
        username={registeredUsername}
      />
    </div>
  )
}
