import { useCallback } from 'react'
import { useNavigate } from 'react-router-dom'
import { User, Mail, Phone, GraduationCap, Check, AlertCircle } from 'lucide-react'
import { useForm } from 'react-hook-form'
import { zodResolver } from '@hookform/resolvers/zod'
import { profileSetupSchema, type ProfileSetupForm } from '@/schemas/profileSchema'
import { userApi } from '@/api/userApi'
import { useQueryClient } from '@tanstack/react-query'
import { useUIStore } from '@/store/uiStore'
import { errorHandler } from '@/utils/errorHandler'
import { cn } from '@/lib/utils'
import type { ComponentType } from 'react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogDescription,
  DialogFooter,
  Input,
  Label,
} from '@/components/ui'
import { Button } from '@/components/ui/button'

interface ProfileSetupModalProps {
  isOpen: boolean
  onClose: () => void
  username: string
}

interface FormField {
  key: keyof ProfileSetupForm
  label: string
  placeholder: string
  icon: ComponentType<{ size?: number; className?: string }>
  type: string
  maxLength?: number
}

const formFields: FormField[] = [
  {
    key: 'realName',
    label: '真实姓名',
    placeholder: '请输入您的真实姓名',
    icon: User,
    type: 'text',
  },
  {
    key: 'studentId',
    label: '学号',
    placeholder: '请输入您的学号',
    icon: GraduationCap,
    type: 'text',
  },
  {
    key: 'email',
    label: '邮箱',
    placeholder: '请输入您的邮箱地址',
    icon: Mail,
    type: 'email',
  },
  {
    key: 'phone',
    label: '手机号',
    placeholder: '请输入您的手机号',
    icon: Phone,
    type: 'tel',
    maxLength: 11,
  },
]

export function ProfileSetupModal({ isOpen, onClose, username }: ProfileSetupModalProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const addToast = useUIStore((s) => s.addToast)

  const { register, handleSubmit: rhfHandleSubmit, watch, formState } = useForm<ProfileSetupForm>({
    resolver: zodResolver(profileSetupSchema),
    reValidateMode: 'onChange',
    mode: 'onTouched',
    defaultValues: { realName: '', studentId: '', email: '', phone: '' },
  })
  const vals = watch()
  const isSubmitting = formState.isSubmitting

  const handleClose = useCallback(() => {
    addToast({ type: 'info', message: '您可以在个人中心随时完善信息' })
    onClose()
    navigate('/')
  }, [addToast, onClose, navigate])

  const onSubmit = rhfHandleSubmit(async (data) => {
    try {
      await userApi.updateProfile({
        email: data.email.trim(),
        phone: data.phone.trim(),
        realName: data.realName.trim(),
        studentId: data.studentId.trim()
      })
      await queryClient.invalidateQueries({ queryKey: ['auth', 'user'] })
      addToast({ type: 'success', message: '个人信息完善成功！' })
      onClose()
      navigate('/')
    } catch (err) {
      const msg = errorHandler.handle(err as Error)
      addToast({ type: 'error', message: msg })
    }
  })

  const progress = Math.round(
    formFields.reduce((count, field) => {
      const value = (vals as Record<string, string>)[field.key] || ''
      if (!value.trim()) return count
      return profileSetupSchema.shape[field.key as keyof typeof profileSetupSchema.shape].safeParse(value).success ? count + 1 : count
    }, 0) / formFields.length * 100
  )

  return (
    <Dialog open={isOpen} onOpenChange={(open) => !open && handleClose()}>
      <DialogContent className="max-w-[560px] w-[92vw] gap-0 p-0 overflow-hidden rounded-3xl bg-white/94 backdrop-blur-2xl border-border/60">
        <DialogHeader className="border-b-0 px-6 pt-6 pb-2">
          <DialogTitle className="text-[1.375rem] font-extrabold tracking-tight">
            完善个人信息
          </DialogTitle>
          <DialogDescription className="text-sm mt-1">
            欢迎加入 EasyOrange，请补充以下信息以开始使用
          </DialogDescription>
        </DialogHeader>

        <div className="pt-4 px-6 pb-2">
          <div
            className="flex items-center gap-3 p-3.5 rounded-2xl mb-6 border"
            style={{
              background: 'linear-gradient(135deg, rgba(249, 115, 22, 0.08) 0%, rgba(251, 113, 133, 0.04) 100%)',
              borderColor: 'rgba(249, 115, 22, 0.12)'
            }}
          >
            <div
              className="flex h-10 w-10 shrink-0 items-center justify-center rounded-full text-white font-bold"
              style={{ background: 'var(--gradient-primary)' }}
            >
              {username?.charAt(0).toUpperCase() || 'U'}
            </div>
            <div>
              <p className="font-bold text-[0.9375rem]">{username}</p>
              <p className="text-xs text-[var(--text-secondary)]">新注册用户</p>
            </div>
            <div className="ml-auto text-right">
              <p className="text-xs text-[var(--text-tertiary)] font-semibold">
                完成度
              </p>
              <p
                className="text-lg font-extrabold"
                style={{ color: progress === 100 ? 'var(--success)' : 'var(--primary-500)' }}
              >
                {progress}%
              </p>
            </div>
          </div>

          <div className="w-full h-1 bg-[var(--gray-100)] rounded-sm mb-6 overflow-hidden">
            <div
              className="h-full rounded-sm transition-all duration-400 ease-out"
              style={{ width: `${progress}%`, background: 'var(--gradient-primary)' }}
            />
          </div>

          <div className="flex flex-col gap-4">
            {formFields.map((field) => {
              const Icon = field.icon
              const error = formState.errors[field.key as keyof ProfileSetupForm]?.message
              const value = (vals as Record<string, string>)[field.key] || ''

              return (
                <div key={field.key}>
                  <Label
                    htmlFor={`profile-${field.key}`}
                    className="block text-[0.8125rem] font-bold text-[var(--text-primary)] mb-1.5"
                  >
                    {field.label}
                    <span className="text-[var(--error)] ml-0.5">*</span>
                  </Label>
                  <div className="relative">
                    <Icon
                      size={18}
                      className={cn(
                        'absolute left-3 top-1/2 -translate-y-1/2 pointer-events-none shrink-0',
                        error ? 'text-[var(--error)]' : value ? 'text-primary-500' : 'text-[var(--text-tertiary)]'
                      )}
                    />
                    <Input
                      id={`profile-${field.key}`}
                      type={field.type}
                      placeholder={field.placeholder}
                      maxLength={field.maxLength}
                      className={cn(
                        'pl-10 pr-10 rounded-xl border-[1.5px] text-[0.9375rem] transition-all',
                        error
                          ? 'border-[var(--error)] bg-[rgba(239,68,68,0.03)]'
                          : value
                            ? 'border-primary-300 bg-[rgba(249,115,22,0.02)]'
                            : 'border-[var(--border-light)] bg-[var(--gray-50)]'
                      )}
                      {...register(field.key)}
                    />
                    {value && !error && (
                      <Check size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--success)] pointer-events-none shrink-0" />
                    )}
                    {error && (
                      <AlertCircle size={16} className="absolute right-3 top-1/2 -translate-y-1/2 text-[var(--error)] pointer-events-none shrink-0" />
                    )}
                  </div>
                  {error && (
                    <p className="text-xs text-[var(--error)] mt-1.5 font-medium">
                      {error}
                    </p>
                  )}
                </div>
              )
            })}
          </div>
        </div>

        <DialogFooter className="border-t border-[var(--border-light)] flex justify-between items-center px-6 py-4 gap-2">
          <Button
            variant="ghost"
            onClick={handleClose}
            className="text-[var(--text-tertiary)] hover:text-[var(--text-secondary)] hover:bg-transparent"
          >
            稍后再说
          </Button>
          <Button
            onClick={onSubmit}
            disabled={isSubmitting}
            isLoading={isSubmitting}
            loadingText="保存中..."
            className="rounded-xl px-8"
          >
            <Check size={18} />
            完成设置
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
