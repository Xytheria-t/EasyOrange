import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { userApi } from '@/api/userApi'
import { ArrowLeft, KeyRound, Smartphone, ShieldCheck } from 'lucide-react'
import { useUIStore } from '@/store/uiStore'
import '@/styles/main.css'

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
      const msg = err instanceof Error ? err.message : '发送验证码失败'
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
      const msg = err instanceof Error ? err.message : '重置密码失败'
      addToast({ type: 'error', message: msg })
    } finally {
      setIsLoading(false)
    }
  }

  const stepConfig = [
    { num: 1, label: '验证手机', icon: Smartphone },
    { num: 2, label: '输入验证码', icon: ShieldCheck },
    { num: 3, label: '重置密码', icon: KeyRound },
  ]

  return (
    <div className="auth-page">
      <div className="auth-page-container">
        <div className="auth-page-card">
          <button className="auth-page-back-btn" onClick={() => navigate('/login')}>
            <ArrowLeft size={18} />
            <span>返回登录</span>
          </button>

          <div className="auth-page-header">
            <h1 className="auth-page-title">忘记密码</h1>
            <p className="auth-page-subtitle">通过手机验证码重置您的密码</p>
          </div>

          <div className="auth-page-steps">
            {stepConfig.map(({ num, label, icon: Icon }, idx) => (
              <div key={num} className="auth-page-step" style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
                <div className={`auth-page-step-dot ${step >= num ? 'active' : ''}`}>
                  <Icon size={14} />
                </div>
                <span className={`auth-page-step-label ${step >= num ? 'active' : ''}`}>{label}</span>
                {idx < stepConfig.length - 1 && <div className={`auth-page-step-line ${step > num ? 'active' : ''}`} />}
              </div>
            ))}
          </div>

          <div className="auth-page-form">
            {step === 1 && (
              <>
                <div className="form-group">
                  <label className="form-label">手机号</label>
                  <input
                    className="form-input"
                    type="tel"
                    placeholder="请输入注册时绑定的手机号"
                    value={phone}
                    onChange={(e) => setPhone(e.target.value)}
                    maxLength={11}
                  />
                </div>
                <button className="btn btn-primary btn-lg" style={{ width: '100%' }} onClick={handleSendCode} disabled={countdown > 0}>
                  {countdown > 0 ? `${countdown}s 后重新发送` : '发送验证码'}
                </button>
              </>
            )}

            {step === 2 && (
              <>
                <div className="form-group">
                  <label className="form-label">验证码</label>
                  <div style={{ display: 'flex', gap: '0.75rem' }}>
                    <input
                      className="form-input"
                      type="text"
                      placeholder="请输入6位验证码"
                      value={verifyCode}
                      onChange={(e) => setVerifyCode(e.target.value)}
                      maxLength={6}
                      style={{ flex: 1 }}
                    />
                    <button
                      className="btn btn-secondary btn-md"
                      onClick={handleSendCode}
                      disabled={countdown > 0}
                      style={{ whiteSpace: 'nowrap' }}
                    >
                      {countdown > 0 ? `${countdown}s` : '重新发送'}
                    </button>
                  </div>
                </div>
                <button className="btn btn-primary btn-lg" style={{ width: '100%' }} onClick={handleVerifyCode}>
                  下一步
                </button>
              </>
            )}

            {step === 3 && (
              <>
                <div className="form-group">
                  <label className="form-label">新密码</label>
                  <input
                    className="form-input"
                    type="password"
                    placeholder="需包含大小写字母和数字，6-20位"
                    value={newPassword}
                    onChange={(e) => setNewPassword(e.target.value)}
                  />
                </div>
                <div className="form-group">
                  <label className="form-label">确认新密码</label>
                  <input
                    className="form-input"
                    type="password"
                    placeholder="再次输入新密码"
                    value={confirmPassword}
                    onChange={(e) => setConfirmPassword(e.target.value)}
                  />
                </div>
                <button className="btn btn-primary btn-lg" style={{ width: '100%' }} onClick={handleResetPassword} disabled={isLoading}>
                  {isLoading ? '重置中...' : '重置密码'}
                </button>
              </>
            )}
          </div>
        </div>
      </div>
    </div>
  )
}
