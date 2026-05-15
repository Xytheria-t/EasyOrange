import { useNavigate } from 'react-router-dom'
import { Shield, ArrowRight } from 'lucide-react'
import { useAdminGuard } from '@/admin/hooks/useAdminGuard'

export function AdminMenuEntry() {
  const navigate = useNavigate()
  const { isAdmin } = useAdminGuard()

  if (!isAdmin) {return null}

  return (
    <button
      className="admin-entry-btn"
      onClick={() => navigate('/admin')}
      aria-label="进入后台管理"
    >
      <Shield size={16} className="admin-entry-icon" />
      <span>后台管理</span>
      <ArrowRight size={14} className="admin-entry-arrow" />
    </button>
  )
}
