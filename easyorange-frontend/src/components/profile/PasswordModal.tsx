import { Smartphone, ShieldCheck } from 'lucide-react'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogFooter,
  Input,
  Label,
} from '@/components/ui'
import { Button } from '@/components/ui/button'

interface PasswordModalProps {
  show: boolean
  form: { verifyCode: string; newPassword: string; confirmPassword: string }
  isLoading: boolean
  countdown: number
  phone: string
  onFormChange: (form: { verifyCode: string; newPassword: string; confirmPassword: string }) => void
  onClose: () => void
  onSubmit: () => void
  onSendCode: () => void
}

export function PasswordModal({
  show,
  form,
  isLoading,
  countdown,
  phone,
  onFormChange,
  onClose,
  onSubmit,
  onSendCode,
}: PasswordModalProps) {
  return (
    <Dialog open={show} onOpenChange={(open) => !open && onClose()}>
      <DialogContent className="gap-0 p-0 overflow-hidden rounded-3xl bg-white/94 backdrop-blur-2xl border-border/60 sm:max-w-[480px]">
        <DialogHeader className="px-6 pt-6 pb-2">
          <DialogTitle className="text-[1.25rem] font-bold tracking-tight">修改密码</DialogTitle>
        </DialogHeader>

        <div className="p-6 space-y-5">
          <div className="space-y-2">
            <Label htmlFor="password-phone">手机号</Label>
            <div className="relative">
              <Smartphone size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
              <Input
                id="password-phone"
                type="tel"
                value={phone}
                disabled
                placeholder="手机号"
                className="pl-10 pr-24 bg-muted/40"
              />
              <Button
                type="button"
                variant="outline"
                size="sm"
                onClick={onSendCode}
                disabled={countdown > 0}
                className="absolute right-1 top-1/2 -translate-y-1/2 h-8 px-3 text-xs rounded-lg"
              >
                {countdown > 0 ? `${countdown}s` : '发送验证码'}
              </Button>
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="change-verify-code">验证码</Label>
            <div className="relative">
              <ShieldCheck size={18} className="absolute left-3 top-1/2 -translate-y-1/2 text-muted-foreground pointer-events-none" />
              <Input
                id="change-verify-code"
                type="text"
                value={form.verifyCode}
                onChange={(e) => onFormChange({ ...form, verifyCode: e.target.value })}
                placeholder="请输入6位验证码"
                maxLength={6}
                className="pl-10"
              />
            </div>
          </div>

          <div className="space-y-2">
            <Label htmlFor="new-password">新密码</Label>
            <Input
              id="new-password"
              type="password"
              value={form.newPassword}
              onChange={(e) => onFormChange({ ...form, newPassword: e.target.value })}
              placeholder="需包含大小写字母和数字，6-20位"
            />
          </div>

          <div className="space-y-2">
            <Label htmlFor="confirm-password">确认新密码</Label>
            <Input
              id="confirm-password"
              type="password"
              value={form.confirmPassword}
              onChange={(e) => onFormChange({ ...form, confirmPassword: e.target.value })}
              placeholder="再次输入新密码"
            />
          </div>
        </div>

        <DialogFooter className="px-6 py-4 border-t border-border/40 gap-2">
          <Button variant="outline" onClick={onClose} className="rounded-xl px-5">
            取消
          </Button>
          <Button
            onClick={onSubmit}
            disabled={isLoading}
            isLoading={isLoading}
            loadingText="修改中..."
            className="rounded-xl px-6"
          >
            确认修改
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
