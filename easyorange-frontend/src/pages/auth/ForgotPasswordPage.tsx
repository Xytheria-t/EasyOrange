import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { userApi } from '@/api/userApi'
import { ArrowLeft, KeyRound, Smartphone, ShieldCheck, Sparkles, Check } from 'lucide-react'
import { useUIStore } from '@/store/uiStore'
import { errorHandler } from '@/utils/errorHandler'
import './forgot-password.css'

type Step = 1 | 2 | 3

export function ForgotPasswordPage() {
  const navigate = useNavigate()
  const addToast = useUIStore((s) => s.addToast)
  const [step, setStep] = useState<Step>(1)
  const [phone, setPhone] = useState('')
  const [verifyCode, setVerifyCode] = useState('')
  const [newPassword, setNewPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [isLoading, setIsLoading] = useState(false)
  const [countdown, setCountdown] = useState(0)

  const startCountdown = () => {
    setCountdown(60)
    const timer = setInterval(() => {
      setCountdown((prev) => {
        if (prev <= 1) {
          clearInterval(timer)
          return 0
        }
        return prev - 1
      })
    }, 1000)
  }

  const handleSendCode = async () => {
    if (!phone) {
      addToast({ type: 'warning', message: '请输入手机号' })
      return
    }
    try {
      await userApi.sendSmsCode(phone)
      startCountdown()
      setStep(2)
      addToast({ type: 'success', message: '验证码已发送' })
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    }
  }

  const handleVerifyCode = () => {
    if (!verifyCode) {
      addToast({ type: 'warning', message: '请输入验证码' })
      return
    }
    setStep(3)
  }

  const handleResetPassword = async () => {
    if (!newPassword) {
      addToast({ type: 'warning', message: '请输入新密码' })
      return
    }
    if (newPassword !== confirmPassword) {
      addToast({ type: 'warning', message: '两次输入的密码不一致' })
      return
    }
    setIsLoading(true)
    try {
      await userApi.forgotPassword({
        phone,
        verifyCode,
        newPassword
      })
      addToast({ type: 'success', message: '密码重置成功，请使用新密码登录' })
      navigate('/login')
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsLoading(false)
    }
  }

  const stepConfig = [
    { num: 1, label: '验证手机', icon: Smartphone, desc: '输入注册手机号' },
    { num: 2, label: '输入验证码', icon: ShieldCheck, desc: '验证身份' },
    { num: 3, label: '重置密码', icon: KeyRound, desc: '设置新密码' },
  ]

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

      <button className="forgot-password-close-btn" onClick={() => navigate('/login')} aria-label="返回登录">
        <svg viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
          <line x1="18" y1="6" x2="6" y2="18" />
          <line x1="6" y1="6" x2="18" y2="18" />
        </svg>
      </button>

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
                <div className={`forgot-password-step-indicator ${step >= num ? 'active' : ''} ${step > num ? 'completed' : ''}`}>
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
                  <label className="form-label">手机号</label>
                  <div className="input-wrapper">
                    <Smartphone size={18} className="input-icon" />
                    <input
                      className="form-input"
                      type="tel"
                      placeholder="请输入注册时绑定的手机号"
                      value={phone}
                      onChange={(e) => setPhone(e.target.value)}
                      maxLength={11}
                      data-testid="input-forgot-phone"
                    />
                  </div>
                </div>
                <button 
                  className="forgot-password-btn" 
                  onClick={handleSendCode} 
                  disabled={countdown > 0}
                  data-testid="btn-send-code"
                >
                  <Sparkles size={16} />
                  {countdown > 0 ? `${countdown}s 后重新发送` : '发送验证码'}
                </button>
              </div>
            )}

            {step === 2 && (
              <div className="forgot-password-form-step">
                <div className="form-group">
                  <label className="form-label">验证码</label>
                  <div className="input-wrapper">
                    <ShieldCheck size={18} className="input-icon" />
                    <input
                      className="form-input"
                      type="text"
                      placeholder="请输入6位验证码"
                      value={verifyCode}
                      onChange={(e) => setVerifyCode(e.target.value)}
                      maxLength={6}
                      data-testid="input-verify-code"
                    />
                    <button
                      className="resend-btn"
                      onClick={handleSendCode}
                      disabled={countdown > 0}
                    >
                      {countdown > 0 ? `${countdown}s` : '重新发送'}
                    </button>
                  </div>
                </div>
                <button className="forgot-password-btn" onClick={handleVerifyCode} data-testid="btn-verify-next">
                  下一步
                </button>
              </div>
            )}

            {step === 3 && (
              <div className="forgot-password-form-step">
                <div className="form-group">
                  <label className="form-label">新密码</label>
                  <div className="input-wrapper">
                    <KeyRound size={18} className="input-icon" />
                    <input
                      className="form-input"
                      type="password"
                      placeholder="需包含大小写字母和数字，6-20位"
                      value={newPassword}
                      onChange={(e) => setNewPassword(e.target.value)}
                      data-testid="input-new-password"
                    />
                  </div>
                </div>
                <div className="form-group">
                  <label className="form-label">确认新密码</label>
                  <div className="input-wrapper">
                    <KeyRound size={18} className="input-icon" />
                    <input
                      className="form-input"
                      type="password"
                      placeholder="再次输入新密码"
                      value={confirmPassword}
                      onChange={(e) => setConfirmPassword(e.target.value)}
                      data-testid="input-confirm-new-password"
                    />
                  </div>
                </div>
                <button 
                  className="forgot-password-btn" 
                  onClick={handleResetPassword} 
                  disabled={isLoading}
                  data-testid="btn-reset-password"
                >
                  {isLoading ? '重置中...' : '重置密码'}
                </button>
              </div>
            )}
          </div>

          <div className="forgot-password-footer">
            <button className="back-to-login-btn" onClick={() => navigate('/login')}>
              <ArrowLeft size={16} />
              返回登录
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
