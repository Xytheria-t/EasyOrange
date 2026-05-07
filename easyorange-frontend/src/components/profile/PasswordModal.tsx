import { X } from 'lucide-react'

interface PasswordModalProps {
  show: boolean
  form: { oldPassword: string; newPassword: string; confirmPassword: string }
  isLoading: boolean
  onFormChange: (form: { oldPassword: string; newPassword: string; confirmPassword: string }) => void
  onClose: () => void
  onSubmit: () => void
}

export function PasswordModal({ show, form, isLoading, onFormChange, onClose, onSubmit }: PasswordModalProps) {
  if (!show) {return null}

  return (
    <div className="modal-overlay active" onClick={onClose}>
      <div
        className="modal modal-content-large"
        style={{ opacity: 1, visibility: 'visible', pointerEvents: 'auto', transform: 'translate(-50%, -50%) scale(1)' }}
        onClick={(e) => e.stopPropagation()}
      >
        <div className="modal-header">
          <h3>修改密码</h3>
          <button className="modal-close" onClick={onClose}>
            <X size={18} />
          </button>
        </div>
        <div className="modal-body">
          <div className="form-group">
            <label className="form-label">旧密码</label>
            <input
              className="form-input"
              type="password"
              value={form.oldPassword}
              onChange={(e) => onFormChange({ ...form, oldPassword: e.target.value })}
              placeholder="请输入旧密码"
            />
          </div>
          <div className="form-group">
            <label className="form-label">新密码</label>
            <input
              className="form-input"
              type="password"
              value={form.newPassword}
              onChange={(e) => onFormChange({ ...form, newPassword: e.target.value })}
              placeholder="需包含大小写字母和数字，6-20位"
            />
          </div>
          <div className="form-group">
            <label className="form-label">确认新密码</label>
            <input
              className="form-input"
              type="password"
              value={form.confirmPassword}
              onChange={(e) => onFormChange({ ...form, confirmPassword: e.target.value })}
              placeholder="再次输入新密码"
            />
          </div>
        </div>
        <div className="modal-footer">
          <button className="btn btn-secondary btn-md" onClick={onClose}>取消</button>
          <button className="btn btn-primary btn-md" onClick={onSubmit} disabled={isLoading}>
            {isLoading ? '修改中...' : '确认修改'}
          </button>
        </div>
      </div>
    </div>
  )
}
