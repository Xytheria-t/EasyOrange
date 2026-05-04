import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { User, Mail, Phone, GraduationCap, Check, AlertCircle, X } from 'lucide-react'
import { userApi } from '@/api/userApi'
import { useQueryClient } from '@tanstack/react-query'
import { useUIStore } from '@/store/uiStore'
import { errorHandler } from '@/utils/errorHandler'
import { openOverlayLayer, closeOverlayLayer } from '@/stores/overlayStore'

interface ProfileSetupModalProps {
  isOpen: boolean
  onClose: () => void
  username: string
}

interface FormField {
  key: string
  label: string
  placeholder: string
  icon: typeof User
  required: boolean
  type: string
  maxLength?: number
  validate?: (value: string) => string | null
}

const OVERLAY_LAYER_ID = 'profile-setup-modal'

const formFields: FormField[] = [
  {
    key: 'realName',
    label: '真实姓名',
    placeholder: '请输入您的真实姓名',
    icon: User,
    required: true,
    type: 'text',
    validate: (value) => {
      if (!value || value.trim().length < 2) return '真实姓名至少需要2个字符'
      if (!/^[\u4e00-\u9fa5a-zA-Z\s]+$/.test(value)) return '姓名只能包含中文、英文字母和空格'
      return null
    }
  },
  {
    key: 'studentId',
    label: '学号',
    placeholder: '请输入您的学号',
    icon: GraduationCap,
    required: true,
    type: 'text',
    validate: (value) => {
      if (!value || value.trim().length < 5) return '请输入有效的学号'
      return null
    }
  },
  {
    key: 'email',
    label: '邮箱',
    placeholder: '请输入您的邮箱地址',
    icon: Mail,
    required: true,
    type: 'email',
    validate: (value) => {
      if (!value) return '邮箱不能为空'
      if (!/^[^\s@]+@[^\s@]+\.[^\s@]+$/.test(value)) return '请输入有效的邮箱地址'
      return null
    }
  },
  {
    key: 'phone',
    label: '手机号',
    placeholder: '请输入您的手机号',
    icon: Phone,
    required: true,
    type: 'tel',
    maxLength: 11,
    validate: (value) => {
      if (!value) return '手机号不能为空'
      if (!/^1[3-9]\d{9}$/.test(value)) return '请输入有效的11位手机号'
      return null
    }
  }
]

export function ProfileSetupModal({ isOpen, onClose, username }: ProfileSetupModalProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const addToast = useUIStore((s) => s.addToast)

  const [formData, setFormData] = useState<Record<string, string>>({
    realName: '',
    studentId: '',
    email: '',
    phone: ''
  })
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [isSubmitting, setIsSubmitting] = useState(false)

  const handleClose = useCallback(() => {
    addToast({ type: 'info', message: '您可以在个人中心随时完善信息' })
    onClose()
    navigate('/')
  }, [addToast, onClose, navigate])

  useEffect(() => {
    if (isOpen) {
      openOverlayLayer(OVERLAY_LAYER_ID)
    } else {
      closeOverlayLayer(OVERLAY_LAYER_ID)
    }
    return () => {
      closeOverlayLayer(OVERLAY_LAYER_ID)
    }
  }, [isOpen])

  useEffect(() => {
    if (!isOpen) return
    const handleKeyDown = (e: KeyboardEvent) => {
      if (e.key === 'Escape') {
        handleClose()
      }
      if (e.key === 'Enter' && !e.shiftKey && !isSubmitting) {
        e.preventDefault()
        handleSubmit()
      }
    }
    document.addEventListener('keydown', handleKeyDown)
    return () => document.removeEventListener('keydown', handleKeyDown)
  }, [isOpen, handleClose, isSubmitting])

  if (!isOpen) return null

  const validateField = (field: FormField, value: string): string | null => {
    if (field.required && !value.trim()) {
      return `${field.label}为必填项`
    }
    if (field.validate && value.trim()) {
      return field.validate(value)
    }
    return null
  }

  const validateAll = (): boolean => {
    const newErrors: Record<string, string> = {}
    let hasError = false

    for (const field of formFields) {
      const error = validateField(field, formData[field.key] || '')
      if (error) {
        newErrors[field.key] = error
        hasError = true
      }
    }

    setErrors(newErrors)
    return !hasError
  }

  const handleChange = (key: string, value: string) => {
    setFormData((prev) => ({ ...prev, [key]: value }))
    if (errors[key]) {
      setErrors((prev) => {
        const next = { ...prev }
        delete next[key]
        return next
      })
    }
  }

  const handleSubmit = async () => {
    if (!validateAll()) {
      addToast({ type: 'warning', message: '请完善所有必填信息' })
      return
    }

    setIsSubmitting(true)
    try {
      await userApi.updateProfile({
        email: formData.email.trim(),
        phone: formData.phone.trim(),
        realName: formData.realName.trim(),
        studentId: formData.studentId.trim()
      })
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      addToast({ type: 'success', message: '个人信息完善成功！' })
      onClose()
      navigate('/')
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsSubmitting(false)
    }
  }

  const validCount = formFields.filter((f) => {
    const value = formData[f.key] || ''
    return value.trim() && !validateField(f, value)
  }).length
  const progress = Math.round((validCount / formFields.length) * 100)

  return (
    <div
      className="modal-overlay active"
      style={{ zIndex: 2000 }}
      onClick={handleClose}
      role="dialog"
      aria-modal="true"
      aria-labelledby="profile-setup-title"
    >
      <div
        className="modal modal-content-large"
        style={{
          opacity: 1,
          visibility: 'visible',
          pointerEvents: 'auto',
          transform: 'translate(-50%, -50%) scale(1)',
          maxWidth: 560,
          width: '92vw',
          minWidth: 'auto'
        }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header" style={{ borderBottom: 'none', paddingBottom: '0.5rem' }}>
          <div>
            <h3 id="profile-setup-title" style={{ fontSize: '1.375rem', fontWeight: 800, letterSpacing: '-0.02em' }}>
              完善个人信息
            </h3>
            <p style={{ fontSize: '0.875rem', color: 'var(--text-secondary)', marginTop: '0.25rem' }}>
              欢迎加入 EasyOrange，请补充以下信息以开始使用
            </p>
          </div>
          <button
            className="modal-close"
            onClick={handleClose}
            aria-label="关闭"
          >
            <X size={18} />
          </button>
        </div>

        <div className="modal-body" style={{ paddingTop: '1rem' }}>
          <div
            style={{
              display: 'flex',
              alignItems: 'center',
              gap: '0.75rem',
              padding: '0.875rem 1rem',
              borderRadius: 'var(--radius-lg)',
              background: 'linear-gradient(135deg, rgba(249, 115, 22, 0.08) 0%, rgba(251, 113, 133, 0.04) 100%)',
              border: '1px solid rgba(249, 115, 22, 0.12)',
              marginBottom: '1.5rem'
            }}
          >
            <div
              style={{
                width: 40,
                height: 40,
                borderRadius: '50%',
                background: 'var(--gradient-primary)',
                display: 'flex',
                alignItems: 'center',
                justifyContent: 'center',
                color: 'white',
                fontSize: '1rem',
                fontWeight: 700,
                flexShrink: 0
              }}
            >
              {username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div>
              <p style={{ fontWeight: 700, fontSize: '0.9375rem' }}>{username}</p>
              <p style={{ fontSize: '0.8125rem', color: 'var(--text-secondary)' }}>新注册用户</p>
            </div>
            <div style={{ marginLeft: 'auto', textAlign: 'right' }}>
              <p style={{ fontSize: '0.75rem', color: 'var(--text-tertiary)', fontWeight: 600 }}>
                完成度
              </p>
              <p
                style={{
                  fontSize: '1.125rem',
                  fontWeight: 800,
                  color: progress === 100 ? 'var(--success)' : 'var(--primary-500)'
                }}
              >
                {progress}%
              </p>
            </div>
          </div>

          <div
            style={{
              width: '100%',
              height: 4,
              background: 'var(--gray-100)',
              borderRadius: 2,
              marginBottom: '1.5rem',
              overflow: 'hidden'
            }}
          >
            <div
              style={{
                width: `${progress}%`,
                height: '100%',
                background: 'var(--gradient-primary)',
                borderRadius: 2,
                transition: 'width 0.4s ease'
              }}
            />
          </div>

          <div style={{ display: 'flex', flexDirection: 'column', gap: '1.125rem' }}>
            {formFields.map((field) => {
              const Icon = field.icon
              const error = errors[field.key]
              const value = formData[field.key] || ''

              return (
                <div key={field.key}>
                  <label
                    htmlFor={`profile-${field.key}`}
                    style={{
                      display: 'block',
                      fontSize: '0.8125rem',
                      fontWeight: 700,
                      color: 'var(--text-primary)',
                      marginBottom: '0.375rem'
                    }}
                  >
                    {field.label}
                    <span style={{ color: 'var(--error)', marginLeft: 2 }}>*</span>
                  </label>
                  <div
                    style={{
                      display: 'flex',
                      alignItems: 'center',
                      gap: '0.75rem',
                      padding: '0.875rem 1rem',
                      borderRadius: 'var(--radius-xl)',
                      border: `1.5px solid ${error ? 'var(--error)' : value ? 'var(--primary-300)' : 'var(--border-light)'}`,
                      background: error
                        ? 'rgba(239, 68, 68, 0.03)'
                        : value
                          ? 'rgba(249, 115, 22, 0.02)'
                          : 'var(--gray-50)',
                      transition: 'all var(--transition-fast)'
                    }}
                  >
                    <Icon
                      size={18}
                      style={{
                        color: error ? 'var(--error)' : value ? 'var(--primary-500)' : 'var(--text-tertiary)',
                        flexShrink: 0
                      }}
                    />
                    <input
                      id={`profile-${field.key}`}
                      type={field.type}
                      value={value}
                      onChange={(e) => handleChange(field.key, e.target.value)}
                      placeholder={field.placeholder}
                      maxLength={field.maxLength}
                      style={{
                        flex: 1,
                        border: 'none',
                        outline: 'none',
                        background: 'transparent',
                        fontSize: '0.9375rem',
                        color: 'var(--text-primary)'
                      }}
                      onBlur={() => {
                        const err = validateField(field, value)
                        if (err) {
                          setErrors((prev) => ({ ...prev, [field.key]: err }))
                        }
                      }}
                    />
                    {value && !error && (
                      <Check size={16} style={{ color: 'var(--success)', flexShrink: 0 }} />
                    )}
                    {error && (
                      <AlertCircle size={16} style={{ color: 'var(--error)', flexShrink: 0 }} />
                    )}
                  </div>
                  {error && (
                    <p style={{ fontSize: '0.75rem', color: 'var(--error)', marginTop: '0.375rem', fontWeight: 500 }}>
                      {error}
                    </p>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        <div
          className="modal-footer"
          style={{
            borderTop: '1px solid var(--border-light)',
            display: 'flex',
            justifyContent: 'space-between',
            alignItems: 'center'
          }}
        >
          <button
            onClick={handleClose}
            style={{
              padding: '0.75rem 1.25rem',
              borderRadius: 'var(--radius-xl)',
              border: 'none',
              background: 'transparent',
              color: 'var(--text-tertiary)',
              fontSize: '0.875rem',
              fontWeight: 600,
              cursor: 'pointer',
              transition: 'all var(--transition-fast)'
            }}
            onMouseEnter={(e) => {
              e.currentTarget.style.color = 'var(--text-secondary)'
            }}
            onMouseLeave={(e) => {
              e.currentTarget.style.color = 'var(--text-tertiary)'
            }}
          >
            稍后再说
          </button>
          <button
            onClick={handleSubmit}
            disabled={isSubmitting}
            style={{
              padding: '0.875rem 2rem',
              borderRadius: 'var(--radius-xl)',
              border: 'none',
              background: 'var(--gradient-primary)',
              color: 'white',
              fontSize: '0.9375rem',
              fontWeight: 700,
              cursor: isSubmitting ? 'not-allowed' : 'pointer',
              opacity: isSubmitting ? 0.7 : 1,
              boxShadow: '0 4px 16px rgba(249, 115, 22, 0.35)',
              transition: 'all var(--transition-fast)',
              display: 'flex',
              alignItems: 'center',
              gap: '0.5rem'
            }}
          >
            {isSubmitting ? (
              <>
                <span
                  style={{
                    width: 18,
                    height: 18,
                    border: '2px solid rgba(255,255,255,0.3)',
                    borderTopColor: 'white',
                    borderRadius: '50%',
                    animation: 'spin 0.7s linear infinite',
                    display: 'inline-block'
                  }}
                />
                保存中...
              </>
            ) : (
              <>
                <Check size={18} />
                完成设置
              </>
            )}
          </button>
        </div>
      </div>
    </div>
  )
}
