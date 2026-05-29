import { useState, useEffect, useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { useCurrentUser, useLogout } from '@/hooks'
import { userApi } from '@/api/userApi'
import { favoriteApi } from '@/api/favoriteApi'
import { useQueryClient } from '@tanstack/react-query'
import { useUIStore } from '@/store/uiStore'
import { errorHandler } from '@/utils/errorHandler'
import {
  ProfileSidebar,
  ProfileOverview,
  ProfileActivity,
  ProfileSecurity,
  ProfilePreferences,
  PasswordModal,
} from '@/components/profile'
import '@/styles/main.css'
import './profile.css'

type EditableField = 'nickname' | 'email' | 'phone' | 'realName' | 'studentId'
type TabType = 'overview' | 'activity' | 'security' | 'preferences'

function ProfilePage() {
  const { data: user, isLoading } = useCurrentUser()
  const logout = useLogout()
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const addToast = useUIStore((s) => s.addToast)

  const [activeTab, setActiveTab] = useState<TabType>('overview')
  const [editingField, setEditingField] = useState<EditableField | null>(null)
  const [editValue, setEditValue] = useState('')
  const [isSaving, setIsSaving] = useState(false)
  const [showPasswordModal, setShowPasswordModal] = useState(false)
  const [passwordForm, setPasswordForm] = useState({ oldPassword: '', newPassword: '', confirmPassword: '' })
  const [isChangingPassword, setIsChangingPassword] = useState(false)
  const [animateIn, setAnimateIn] = useState(false)
  const [favoriteCount, setFavoriteCount] = useState(0)

  useEffect(() => {
    const timer = setTimeout(() => setAnimateIn(true), 100)
    return () => clearTimeout(timer)
  }, [])

  useEffect(() => {
    favoriteApi.getCount()
      .then((res) => setFavoriteCount(res.data ?? 0))
      .catch(() => {})
  }, [])

  const handleEdit = useCallback((field: EditableField, currentValue: string) => {
    setEditingField(field)
    setEditValue(currentValue || '')
  }, [])

  const handleSave = useCallback(async () => {
    if (!editingField) {return}
    setIsSaving(true)
    try {
      const data: Record<string, unknown> = {}
      data[editingField] = editValue
      await userApi.updateProfile(data as { nickname?: string; email?: string; phone?: string; realName?: string; studentId?: string })
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      setEditingField(null)
      addToast({ type: 'success', message: '更新成功' })
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsSaving(false)
    }
  }, [editingField, editValue, queryClient, addToast])

  const handleCancelEdit = useCallback(() => {
    setEditingField(null)
    setEditValue('')
  }, [])

  const handleChangePassword = useCallback(async () => {
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
      logout()
      navigate('/login')
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    } finally {
      setIsChangingPassword(false)
    }
  }, [passwordForm, addToast, logout, navigate])

  const handleLogout = useCallback(async () => {
    await logout()
    addToast({ type: 'success', message: '已退出登录' })
    navigate('/')
  }, [logout, navigate, addToast])

  if (isLoading) {
    return (
      <div className="profile-body">
        <div className="profile-main">
          <div className="loading-container">
            <div className="loading-spinner-lg"></div>
            <p className="loading-text">加载中...</p>
          </div>
        </div>
      </div>
    )
  }

  return (
    <div className="profile-body">
      <div className="ambient-bg">
        <div className="gradient-orb orb-1"></div>
        <div className="gradient-orb orb-2"></div>
        <div className="gradient-orb orb-3"></div>
        <div className="noise-overlay"></div>
      </div>

      <div className="profile-main">
        <div className="profile-grid">
          <ProfileSidebar
            user={user}
            activeTab={activeTab}
            onTabChange={setActiveTab}
            onLogout={handleLogout}
            animateIn={animateIn}
          />

          <main className={`profile-content ${animateIn ? 'animate-in-delayed' : ''}`}>
            {activeTab === 'overview' && (
              <ProfileOverview
                user={user}
                favoriteCount={favoriteCount}
                editingField={editingField}
                editValue={editValue}
                isSaving={isSaving}
                onEdit={handleEdit}
                onSave={handleSave}
                onCancel={handleCancelEdit}
                onEditValueChange={setEditValue}
              />
            )}
            {activeTab === 'activity' && <ProfileActivity />}
            {activeTab === 'security' && (
              <ProfileSecurity
                user={user}
                onEdit={handleEdit}
                onShowPasswordModal={() => setShowPasswordModal(true)}
              />
            )}
            {activeTab === 'preferences' && <ProfilePreferences />}
          </main>
        </div>
      </div>

      <PasswordModal
        show={showPasswordModal}
        form={passwordForm}
        isLoading={isChangingPassword}
        onFormChange={setPasswordForm}
        onClose={() => setShowPasswordModal(false)}
        onSubmit={handleChangePassword}
      />
    </div>
  )
}

export default ProfilePage;
