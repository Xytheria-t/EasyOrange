import { useState, useRef } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCurrentUser, useLogout } from '@/hooks'
import { userApi } from '@/api/userApi'
import { useQueryClient } from '@tanstack/react-query'
import { User, Mail, Phone, Calendar, Shield, Camera, KeyRound, LogOut, X, Check, Pencil } from 'lucide-react'
import { useUIStore } from '@/store/uiStore'
import '@/styles/main.css'

type EditableField = 'email' | 'phone' | 'gender'

export function ProfilePage() {
  const { data: user, isLoading } = useCurrentUser()
  const logoutMutation = useLogout()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const fileInputRef = useRef<HTMLInputElement>(null)
  const addToast = useUIStore((s) => s.addToast)

  const [editingField, setEditingField] = useState<EditableField | null>(null)
  const [editValue, setEditValue] = useState('')
  const [isSaving, setIsSaving] = useState(false)

  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [isChangingPassword, setIsChangingPassword] = useState(false)

  const handleEdit = (field: EditableField, currentValue: string) => {
    setEditingField(field)
    setEditValue(currentValue || '')
  }

  const handleSave = async () => {
    if (!editingField) return
    setIsSaving(true)
    try {
      const data: Record<string, unknown> = {}
      data[editingField] = editValue
      await userApi.updateProfile(data as { email?: string; phone?: string; gender?: number })
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      setEditingField(null)
      addToast({ type: 'success', message: '更新成功' })
    } catch (err) {
      const msg = err instanceof Error ? err.message : '更新失败'
      addToast({ type: 'error', message: msg })
    } finally {
      setIsSaving(false)
    }
  }

  const handleCancelEdit = () => {
    setEditingField(null)
    setEditValue('')
  }

  const handleAvatarClick = () => {
    fileInputRef.current?.click()
  }

  const handleAvatarChange = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    if (!file) return
    if (file.size > 5 * 1024 * 1024) {
      addToast({ type: 'warning', message: '头像文件不能超过 5MB' })
      return
    }
    try {
      await userApi.uploadAvatar(file)
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      addToast({ type: 'success', message: '头像更新成功' })
    } catch (err) {
      const msg = err instanceof Error ? err.message : '上传失败'
      addToast({ type: 'error', message: msg })
    }
  }

  const handleChangePassword = async () => {
    if (!passwordForm.oldPassword || !passwordForm.newPassword) {
      addToast({ type: 'warning', message: '请填写完整信息' })
      return
    }
    if (passwordForm.newPassword !== passwordForm.confirmPassword) {
      addToast({ type: 'warning', message: '两次输入的新密码不一致' })
      return
    }
    setIsChangingPassword(true)
    try {
      await userApi.changePassword({
        oldPassword: passwordForm.oldPassword,
        newPassword: passwordForm.newPassword
      })
      setShowPasswordModal(false)
      setPasswordForm({ oldPassword: '', newPassword: '', confirmPassword: '' })
      addToast({ type: 'success', message: '密码修改成功，请重新登录' })
      logoutMutation.mutateAsync()
      navigate('/login')
    } catch (err) {
      const msg = err instanceof Error ? err.message : '修改密码失败'
      addToast({ type: 'error', message: msg })
    } finally {
      setIsChangingPassword(false)
    }
  }

  const handleLogout = async () => {
    try {
      await logoutMutation.mutateAsync()
      navigate('/')
    } catch {
      navigate('/')
    }
  }

  if (isLoading) {
    return (
      <div className="container py-6">
        <div className="loading-container">
          <div className="loading-spinner-lg"></div>
          <p className="loading-text">加载中...</p>
        </div>
      </div>
    )
  }

  const editableFields: { key: EditableField; label: string; value?: string | null; icon: typeof User }[] = [
    { key: 'email', label: '邮箱', value: user?.email, icon: Mail },
    { key: 'phone', label: '手机', value: user?.phone, icon: Phone },
  ]

  const readonlyFields = [
    { key: 'username', label: '用户名', value: user?.username, icon: User },
    { key: 'studentId', label: '学号', value: user?.studentId, icon: Shield },
    { key: 'createTime', label: '注册时间', value: user?.createTime, icon: Calendar },
  ]

  return (
    <div className="container py-6">
      <div className="mb-6">
        <h1 className="page-title-lg">个人中心</h1>
        <p className="page-subtitle">管理您的个人信息</p>
      </div>

      <div className="card-elevated">
        <div className="profile-header">
          <div className="profile-avatar-wrapper" onClick={handleAvatarClick} style={{ position: 'relative', cursor: 'pointer' }}>
            {user?.avatar ? (
              <img src={user.avatar} alt="头像" className="profile-avatar" style={{ objectFit: 'cover' }} />
            ) : (
              <div className="profile-avatar">
                {user?.username?.charAt(0).toUpperCase() || 'U'}
              </div>
            )}
            <div className="profile-avatar-overlay">
              <Camera size={18} />
            </div>
            <input
              ref={fileInputRef}
              type="file"
              accept="image/*"
              style={{ display: 'none' }}
              onChange={handleAvatarChange}
            />
          </div>
          <div className="profile-info">
            <h2 className="profile-name">{user?.username || '用户'}</h2>
            <p className="profile-welcome">欢迎回来</p>
          </div>
        </div>

        <div className="profile-fields">
          {readonlyFields.map(({ key, label, value, icon: Icon }) => (
            <div className="profile-field" key={key}>
              <div className="profile-field-icon">
                <Icon size={20} />
              </div>
              <div className="profile-field-content">
                <p className="profile-field-label">{label}</p>
                <p className="profile-field-value">{value || '未设置'}</p>
              </div>
            </div>
          ))}
          {editableFields.map(({ key, label, value, icon: Icon }) => (
            <div className="profile-field" key={key}>
              <div className="profile-field-icon">
                <Icon size={20} />
              </div>
              <div className="profile-field-content">
                <p className="profile-field-label">{label}</p>
                {editingField === key ? (
                  <div style={{ display: 'flex', gap: '0.5rem', alignItems: 'center' }}>
                    <input
                      className="form-input"
                      style={{ padding: '0.375rem 0.75rem', fontSize: '0.875rem', flex: 1 }}
                      value={editValue}
                      onChange={(e) => setEditValue(e.target.value)}
                      autoFocus
                      onKeyDown={(e) => { if (e.key === 'Enter') handleSave(); if (e.key === 'Escape') handleCancelEdit(); }}
                    />
                    <button className="profile-action-btn profile-action-save" onClick={handleSave} disabled={isSaving}>
                      <Check size={16} />
                    </button>
                    <button className="profile-action-btn profile-action-cancel" onClick={handleCancelEdit}>
                      <X size={16} />
                    </button>
                  </div>
                ) : (
                  <p className="profile-field-value">{value || '未设置'}</p>
                )}
              </div>
              {!editingField && (
                <button className="profile-edit-btn" onClick={() => handleEdit(key, value || '')}>
                  <Pencil size={14} />
                </button>
              )}
            </div>
          ))}
        </div>

        <div className="profile-actions-section">
          <button className="profile-action-card" onClick={() => setShowPasswordModal(true)}>
            <KeyRound size={20} />
            <span>修改密码</span>
          </button>
          <button className="profile-action-card profile-action-logout" onClick={handleLogout}>
            <LogOut size={20} />
            <span>退出登录</span>
          </button>
        </div>
      </div>

      {showPasswordModal && (
        <div className="modal-overlay active" onClick={() => setShowPasswordModal(false)}>
          <div className="modal modal-content-large" style={{ opacity: 1, visibility: 'visible', pointerEvents: 'auto', transform: 'translate(-50%, -50%) scale(1)' }} onClick={(e) => e.stopPropagation()}>
            <div className="modal-header">
              <h3>修改密码</h3>
              <button className="modal-close" onClick={() => setShowPasswordModal(false)}>
                <X size={18} />
              </button>
            </div>
            <div className="modal-body">
              <div className="form-group">
                <label className="form-label">旧密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.oldPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, oldPassword: e.target.value }))}
                  placeholder="请输入旧密码"
                />
              </div>
              <div className="form-group">
                <label className="form-label">新密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.newPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, newPassword: e.target.value }))}
                  placeholder="需包含大小写字母和数字，6-20位"
                />
              </div>
              <div className="form-group">
                <label className="form-label">确认新密码</label>
                <input
                  className="form-input"
                  type="password"
                  value={passwordForm.confirmPassword}
                  onChange={(e) => setPasswordForm((p) => ({ ...p, confirmPassword: e.target.value }))}
                  placeholder="再次输入新密码"
                />
              </div>
            </div>
            <div className="modal-footer">
              <button className="btn btn-secondary btn-md" onClick={() => setShowPasswordModal(false)}>取消</button>
              <button className="btn btn-primary btn-md" onClick={handleChangePassword} disabled={isChangingPassword}>
                {isChangingPassword ? '修改中...' : '确认修改'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
