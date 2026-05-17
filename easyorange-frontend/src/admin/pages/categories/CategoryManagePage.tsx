import { useState, useCallback, useMemo } from 'react';
import { createPortal } from 'react-dom';
import { ConfirmModal } from '../../components/ConfirmModal';
import { AdminSelect } from '../../components/AdminSelect';
import {
  useAdminCategoryTree,
  useCreateCategory,
  useUpdateCategory,
  useUpdateCategoryStatus,
  useDeleteCategory,
} from '../../hooks';
import type { CategoryTreeVO, CategoryCreateRequest, CategoryUpdateRequest } from '../../types/admin';

type SortField = 'name' | 'sortOrder';
type SortDir = 'asc' | 'desc';

const statusFilterOptions = [
  { value: '', label: '全部状态' },
  { value: '1', label: '启用' },
  { value: '0', label: '禁用' },
];

const levelLabels = ['', '一级', '二级', '三级'];

export default function CategoryManagePage() {
  const { data: treeData, isLoading, isError } = useAdminCategoryTree();
  const createMutation = useCreateCategory();
  const updateMutation = useUpdateCategory();
  const updateStatusMutation = useUpdateCategoryStatus();
  const deleteMutation = useDeleteCategory();

  const [statusFilter, setStatusFilter] = useState('');
  const [searchInput, setSearchInput] = useState('');
  const [sortField, setSortField] = useState<SortField>('sortOrder');
  const [sortDir, setSortDir] = useState<SortDir>('asc');
  const [expandedIds, setExpandedIds] = useState<Set<number>>(new Set());

  // Create modal
  const [createOpen, setCreateOpen] = useState(false);
  const [createName, setCreateName] = useState('');
  const [createParentId, setCreateParentId] = useState<number | undefined>(undefined);
  const [createSortOrder, setCreateSortOrder] = useState(0);

  // Edit modal
  const [editOpen, setEditOpen] = useState(false);
  const [editId, setEditId] = useState<number | null>(null);
  const [editName, setEditName] = useState('');
  const [editParentId, setEditParentId] = useState<number | undefined>(undefined);
  const [editSortOrder, setEditSortOrder] = useState(0);
  const [editStatus, setEditStatus] = useState(1);

  // Delete confirm
  const [deleteTarget, setDeleteTarget] = useState<CategoryTreeVO | null>(null);

  // Toggle expand/collapse
  const toggleExpand = useCallback((id: number) => {
    setExpandedIds((prev) => {
      const next = new Set(prev);
      if (next.has(id)) {next.delete(id);} else {next.add(id);}
      return next;
    });
  }, []);

  // Note: expand/collapse state defaults to empty (all collapsed).
  // Users can click the chevron to expand categories as needed.

  // Collect flat list for parent dropdown
  const allCategories = useMemo(() => {
    const flatten = (nodes: CategoryTreeVO[]): CategoryTreeVO[] => {
      const result: CategoryTreeVO[] = [];
      for (const node of nodes) {
        result.push(node);
        if (node.children && node.children.length > 0) {
          result.push(...flatten(node.children));
        }
      }
      return result;
    };
    return treeData ? flatten(treeData) : [];
  }, [treeData]);

  // Filter and sort tree
  const filteredTree = useMemo(() => {
    if (!treeData) {return [];}

    const filterNode = (node: CategoryTreeVO): CategoryTreeVO | null => {
      const matchStatus = !statusFilter || node.status === Number(statusFilter);
      const matchSearch = !searchInput || node.name.toLowerCase().includes(searchInput.toLowerCase());
      const selfMatch = matchStatus && matchSearch;

      let filteredChildren: CategoryTreeVO[] = [];
      if (node.children && node.children.length > 0) {
        filteredChildren = node.children
          .map(filterNode)
          .filter((n): n is CategoryTreeVO => n !== null);
      }

      if (!selfMatch && filteredChildren.length === 0) {return null;}
      return { ...node, children: filteredChildren };
    };

    const sortNodes = (nodes: CategoryTreeVO[]): CategoryTreeVO[] => {
      const sorted = [...nodes].sort((a, b) => {
        let cmp: number;
        if (sortField === 'name') {
          cmp = a.name.localeCompare(b.name);
        } else {
          cmp = a.sortOrder - b.sortOrder;
        }
        return sortDir === 'asc' ? cmp : -cmp;
      });
      return sorted.map((n) => ({ ...n, children: n.children ? sortNodes(n.children) : [] }));
    };

    return sortNodes(treeData.map(filterNode).filter((n): n is CategoryTreeVO => n !== null));
  }, [treeData, statusFilter, searchInput, sortField, sortDir]);

  // Create handler
  const handleCreate = useCallback(async () => {
    if (!createName.trim()) {return;}
    const data: CategoryCreateRequest = {
      name: createName.trim(),
      sortOrder: createSortOrder,
    };
    if (createParentId !== undefined) {
      data.parentId = createParentId;
    }
    await createMutation.mutateAsync(data);
    setCreateOpen(false);
    setCreateName('');
    setCreateParentId(undefined);
    setCreateSortOrder(0);
  }, [createName, createParentId, createSortOrder, createMutation]);

  // Edit handler
  const handleEdit = useCallback(async () => {
    if (!editId || !editName.trim()) {return;}
    const data: CategoryUpdateRequest = {
      name: editName.trim(),
      sortOrder: editSortOrder,
      status: editStatus,
    };
    if (editParentId !== undefined) {
      data.parentId = editParentId;
    } else {
      data.parentId = null as unknown as undefined;
    }
    await updateMutation.mutateAsync({ id: editId, data });
    setEditOpen(false);
    setEditId(null);
  }, [editId, editName, editParentId, editSortOrder, editStatus, updateMutation]);

  // Status toggle
  const handleToggleStatus = useCallback(async (id: number, currentStatus: number) => {
    await updateStatusMutation.mutateAsync({ id, status: currentStatus === 1 ? 0 : 1 });
  }, [updateStatusMutation]);

  // Delete handler
  const handleDelete = useCallback(async () => {
    if (!deleteTarget) {return;}
    await deleteMutation.mutateAsync(deleteTarget.categoryId);
    setDeleteTarget(null);
  }, [deleteTarget, deleteMutation]);

  // Open edit modal
  const openEdit = useCallback((node: CategoryTreeVO) => {
    setEditId(node.categoryId);
    setEditName(node.name);
    setEditSortOrder(node.sortOrder);
    setEditStatus(node.status);
    // Find parent ID from flattened list
    // Since CategoryTreeVO doesn't have parentId, we use the flat list
    setEditParentId(undefined);
    setEditOpen(true);
  }, []);

  // Count total categories
  const totalCount = allCategories.length;

  // Render tree node recursively
  function renderTreeNode(node: CategoryTreeVO, depth: number) {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedIds.has(node.categoryId);
    const isEnabled = node.status === 1;
    const paddingLeft = 16 + depth * 28;

    return (
      <div key={node.categoryId}>
        {/* Node row */}
        <div
          style={{
            display: 'flex', alignItems: 'center', gap: '0.5rem',
            padding: '0.6rem 1rem 0.6rem 0',
            paddingLeft,
            borderBottom: '1px solid rgba(229,224,219,0.35)',
            transition: 'background 0.15s ease',
            cursor: 'default',
          }}
          onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.03)'; }}
          onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
        >
          {/* Expand/collapse */}
          <button
            type="button"
            onClick={() => toggleExpand(node.categoryId)}
            style={{
              width: 20, height: 20, flexShrink: 0,
              display: 'flex', alignItems: 'center', justifyContent: 'center',
              border: 'none', background: 'transparent',
              cursor: hasChildren ? 'pointer' : 'default',
              color: hasChildren ? '#9B9590' : 'transparent',
              transition: 'transform 0.2s ease',
              transform: isExpanded ? 'rotate(90deg)' : 'rotate(0deg)',
              visibility: hasChildren ? 'visible' : 'hidden',
            }}
            aria-label={isExpanded ? '折叠' : '展开'}
          >
            <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
              <polyline points="9 18 15 12 9 6" />
            </svg>
          </button>

          {/* Level badge */}
          <span style={{
            display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
            minWidth: 32, height: 20, borderRadius: 6,
            fontSize: '0.7rem', fontWeight: 600,
            color: depth === 0 ? '#EA580C' : depth === 1 ? '#2563EB' : '#7C3AED',
            background: depth === 0 ? 'rgba(249,115,22,0.08)' : depth === 1 ? 'rgba(37,99,235,0.08)' : 'rgba(124,58,237,0.08)',
            flexShrink: 0, whiteSpace: 'nowrap',
          }}>
            {levelLabels[depth] || `L${depth + 1}`}
          </span>

          {/* Name */}
          <span style={{
            flex: 1, minWidth: 0,
            fontSize: '0.89rem', fontWeight: 600, color: '#2A2520',
            overflow: 'hidden', textOverflow: 'ellipsis', whiteSpace: 'nowrap',
          }}>
            {node.name}
          </span>

          {/* Sort order */}
          {node.sortOrder > 0 && (
            <span style={{
              fontSize: '0.76rem', color: '#B5AEA8', flexShrink: 0,
              marginRight: '0.25rem',
            }}>
              排序 {node.sortOrder}
            </span>
          )}

          {/* Status */}
          <span style={{
            display: 'inline-flex', alignItems: 'center', gap: '0.3rem',
            padding: '0.22rem 0.6rem', borderRadius: 20,
            fontSize: '0.75rem', fontWeight: 600,
            color: isEnabled ? '#059669' : '#9B9590',
            background: isEnabled ? 'rgba(5,150,105,0.08)' : 'rgba(155,149,144,0.1)',
            flexShrink: 0, whiteSpace: 'nowrap',
          }}>
            <span style={{
              width: 6, height: 6, borderRadius: '50%',
              background: isEnabled ? '#10B981' : '#D6CEC5',
            }} />
            {isEnabled ? '启用' : '禁用'}
          </span>

          {/* Actions */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', flexShrink: 0 }}>
            {/* Toggle status */}
            <button
              type="button"
              title={isEnabled ? '禁用' : '启用'}
              onClick={() => handleToggleStatus(node.categoryId, node.status)}
              disabled={updateStatusMutation.isPending}
              style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 30, height: 30, borderRadius: 8,
                border: 'none', background: 'transparent',
                color: isEnabled ? '#F59E0B' : '#10B981',
                cursor: 'pointer', transition: 'all 0.15s ease',
                opacity: updateStatusMutation.isPending ? 0.5 : 1,
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(42,37,32,0.05)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; }}
            >
              {isEnabled ? (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M17.94 17.94A10.07 10.07 0 0112 20c-7 0-11-8-11-8a18.45 18.45 0 015.06-5.94M9.9 4.24A9.12 9.12 0 0112 4c7 0 11 8 11 8a18.5 18.5 0 01-2.16 3.19m-6.72-1.07a3 3 0 11-4.24-4.24" />
                  <line x1="1" y1="1" x2="23" y2="23" />
                </svg>
              ) : (
                <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                  <circle cx="12" cy="12" r="3" />
                </svg>
              )}
            </button>

            {/* Edit */}
            <button
              type="button"
              title="编辑"
              onClick={() => openEdit(node)}
              style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 30, height: 30, borderRadius: 8,
                border: 'none', background: 'transparent',
                color: '#6B6460', cursor: 'pointer', transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(249,115,22,0.08)'; e.currentTarget.style.color = '#EA580C'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#6B6460'; }}
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
              </svg>
            </button>

            {/* Delete */}
            <button
              type="button"
              title="删除"
              onClick={() => setDeleteTarget(node)}
              style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 30, height: 30, borderRadius: 8,
                border: 'none', background: 'transparent',
                color: '#6B6460', cursor: 'pointer', transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.background = 'rgba(244,63,94,0.08)'; e.currentTarget.style.color = '#E11D48'; }}
              onMouseLeave={(e) => { e.currentTarget.style.background = 'transparent'; e.currentTarget.style.color = '#6B6460'; }}
            >
              <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                <polyline points="3 6 5 6 21 6" />
                <path d="M19 6v14a2 2 0 01-2 2H7a2 2 0 01-2-2V6m3 0V4a2 2 0 012-2h4a2 2 0 012 2v2" />
              </svg>
            </button>
          </div>
        </div>

        {/* Children */}
        {hasChildren && isExpanded && (
          <div>
            {node.children.map((child) => renderTreeNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  }

  // Parent options for create/edit
  const parentOptions = useMemo(() => {
    const options: { value: string; label: string }[] = [
      { value: '', label: '无（一级分类）' },
    ];
    for (const cat of allCategories) {
      const indent = '— '.repeat(cat.level || 0);
      options.push({
        value: String(cat.categoryId),
        label: `${indent}${cat.name}`,
      });
    }
    return options;
  }, [allCategories]);

  return (
    <div
      style={{
        position: 'relative',
        minHeight: 'calc(100vh - 80px)',
        animation: 'pageIn 0.5s ease-out both',
      }}
    >
      {/* Background atmosphere */}
      <div
        style={{
          position: 'absolute', inset: 0, zIndex: 0, pointerEvents: 'none',
          borderRadius: 20,
          background: `
            radial-gradient(ellipse 55% 35% at 8% 12%, rgba(249,115,22,0.035) 0%, transparent 50%),
            radial-gradient(ellipse 40% 45% at 92% 85%, rgba(195,155,211,0.03) 0%, transparent 48%),
            linear-gradient(180deg, #FAF8F5 0%, #FDF9F6 40%, #FAF8F5 100%)
          `,
        }}
      />

      {/* Content */}
      <div style={{ position: 'relative', zIndex: 1, display: 'flex', flexDirection: 'column' }}>

        {isError && (
          <div style={{
            background: 'linear-gradient(135deg, rgba(244,63,94,0.06), rgba(244,63,94,0.02))',
            border: '1px solid rgba(244,63,94,0.12)', borderRadius: 16,
            padding: '1rem 1.25rem', marginBottom: '1.5rem',
            display: 'flex', alignItems: 'center', gap: '0.75rem',
          }}>
            <div style={{
              width: 36, height: 36, borderRadius: 10,
              background: 'linear-gradient(135deg, rgba(244,63,94,0.12), rgba(244,63,94,0.06))',
              display: 'flex', alignItems: 'center', justifyContent: 'center', flexShrink: 0,
            }}>
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="#E11D48" strokeWidth="2"><circle cx="12" cy="12" r="10"/><line x1="12" y1="8" x2="12" y2="12"/><line x1="12" y1="16" x2="12.01" y2="16"/></svg>
            </div>
            <div style={{ flex: 1 }}>
              <div style={{ fontSize: '0.875rem', fontWeight: 600, color: '#E11D48' }}>数据加载失败</div>
              <div style={{ fontSize: '0.8rem', color: '#9B9590' }}>无法连接到服务器，请检查后端服务是否启动</div>
            </div>
            <button onClick={() => window.location.reload()} style={{
              padding: '0.45rem 1rem', borderRadius: 10, background: 'linear-gradient(135deg, #F43F5E, #E11D48)',
              color: '#fff', fontSize: '0.8rem', fontWeight: 600, border: 'none', cursor: 'pointer',
              boxShadow: '0 2px 8px rgba(244,63,94,0.25)',
            }}>刷新</button>
          </div>
        )}

        {/* ===== Header ===== */}
        <header style={{ marginBottom: '1.75rem', animation: 'headerSlide 0.6s ease-out both' }}>
          <div style={{ display: 'flex', alignItems: 'flex-end', justifyContent: 'space-between', gap: '1rem', flexWrap: 'wrap' }}>
            <div>
              <h1 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: 'clamp(1.5rem, 2.5vw, 1.875rem)', fontWeight: 700,
                color: '#2A2520', letterSpacing: '-0.03em', lineHeight: 1.2,
                display: 'flex', alignItems: 'center', gap: '0.65rem',
              }}>
                <span style={{
                  width: 30, height: 30, borderRadius: 10,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  background: 'linear-gradient(135deg, #F97316, #FB923C)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
                    <path d="M4 7V4h16v3" />
                    <path d="M9 20h6" />
                    <path d="M12 4v16" />
                  </svg>
                </span>
                分类管理
              </h1>
              <p style={{ fontSize: '0.88rem', color: '#9B9590', marginTop: '0.3rem', paddingLeft: '36px' }}>
                管理商品分类结构，支持添加、编辑、删除操作
              </p>
            </div>

            {/* Add button */}
            <button
              type="button"
              onClick={() => setCreateOpen(true)}
              style={{
                display: 'inline-flex', alignItems: 'center', gap: '0.4rem',
                padding: '0.68rem 1.2rem', border: 'none', borderRadius: 14,
                background: 'linear-gradient(135deg, #F97316, #EA580C)',
                color: '#fff', fontSize: '0.87rem', fontWeight: 600,
                cursor: 'pointer', transition: 'all 0.2s ease',
                boxShadow: '0 2px 8px rgba(249,115,22,0.28)',
                whiteSpace: 'nowrap', letterSpacing: '0.01em',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.transform = 'translateY(-1px)'; e.currentTarget.style.boxShadow = '0 4px 14px rgba(249,115,22,0.38)'; }}
              onMouseLeave={(e) => { e.currentTarget.style.transform = 'translateY(0)'; e.currentTarget.style.boxShadow = '0 2px 8px rgba(249,115,22,0.28)'; }}
            >
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <line x1="12" y1="5" x2="12" y2="19" />
                <line x1="5" y1="12" x2="19" y2="12" />
              </svg>
              添加分类
            </button>
          </div>
        </header>

        {/* ===== Filter Toolbar ===== */}
        <div style={{
          display: 'flex', alignItems: 'center', flexWrap: 'wrap', gap: '0.75rem',
          padding: '1rem 1.25rem',
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(16px)',
          WebkitBackdropFilter: 'blur(16px)',
          border: '1px solid rgba(255,255,255,0.6)', borderRadius: 16,
          marginBottom: '1.25rem', animation: 'toolbarIn 0.5s ease-out 0.08s both',
        }}>
          {/* Status filter */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><polygon points="22 3 2 3 10 12.46 10 19 14 21 14 12.46 22 3"/></svg>
              状态
            </span>
            <AdminSelect
              options={statusFilterOptions}
              value={statusFilter}
              onChange={(val) => setStatusFilter(val)}
            />
          </div>

          {/* Sort */}
          <div style={{ display: 'flex', alignItems: 'center', gap: '0.45rem' }}>
            <span style={{ display: 'flex', alignItems: 'center', gap: '0.3rem', fontSize: '0.8rem', fontWeight: 500, color: '#9B9590' }}>
              <svg width="13" height="13" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round"><line x1="12" y1="5" x2="12" y2="19"/><polyline points="19 12 12 19 5 12"/></svg>
              排序
            </span>
            <AdminSelect
              options={[
                { value: 'sortOrder', label: '按排序值' },
                { value: 'name', label: '按名称' },
              ]}
              value={sortField}
              onChange={(val) => setSortField(val as SortField)}
            />
            <button
              type="button"
              onClick={() => setSortDir((d) => (d === 'asc' ? 'desc' : 'asc'))}
              title={sortDir === 'asc' ? '升序' : '降序'}
              style={{
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                width: 30, height: 30, borderRadius: 8,
                border: '1.5px solid #E5E0DB', background: '#fff',
                color: '#6B6460', cursor: 'pointer',
                transition: 'all 0.15s ease',
              }}
              onMouseEnter={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.color = '#EA580C'; }}
              onMouseLeave={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.color = '#6B6460'; }}
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round"
                style={{ transform: sortDir === 'desc' ? 'rotate(180deg)' : 'none', transition: 'transform 0.2s ease' }}
              >
                <polyline points="18 15 12 9 6 15" />
              </svg>
            </button>
          </div>

          {/* Search */}
          <div style={{ position: 'relative', width: 200 }}>
            <input
              type="text"
              value={searchInput}
              onChange={(e) => setSearchInput(e.target.value)}
              placeholder="搜索分类名称..."
              style={{
                width: '100%', padding: '0.5rem 0.85rem 0.5rem 2.2rem',
                border: '1.5px solid #E5E0DB', borderRadius: 12,
                background: '#fff', fontSize: '0.82rem', color: '#2A2520',
                outline: 'none', boxShadow: '0 1px 3px rgba(42,37,32,0.04)',
                transition: 'all 0.2s ease',
              }}
              onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08)'; }}
              onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = '0 1px 3px rgba(42,37,32,0.04)'; }}
            />
            <svg style={{
              position: 'absolute', left: '0.7rem', top: '50%', transform: 'translateY(-50%)',
              width: 15, height: 15, color: '#B5AEA8', pointerEvents: 'none',
            }} viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2" strokeLinecap="round" strokeLinejoin="round">
              <circle cx="11" cy="11" r="8" /><line x1="21" y1="21" x2="16.65" y2="16.65" />
            </svg>
          </div>

          {/* Divider */}
          <div style={{ width: 1, height: 20, background: '#E5E0DB', flexShrink: 0 }} />

          {/* Count */}
          <span style={{ fontSize: '0.81rem', color: '#9B9590', fontWeight: 500 }}>
            共 <strong style={{ color: '#2A2520' }}>{totalCount.toLocaleString()}</strong> 个分类
          </span>
        </div>

        {/* ===== Tree Content ===== */}
        <div style={{
          background: 'rgba(255,255,255,0.72)', backdropFilter: 'blur(20px)',
          WebkitBackdropFilter: 'blur(20px)',
          border: '1px solid rgba(255,255,255,0.65)', borderRadius: 20,
          overflow: 'hidden',
          transition: 'all 0.35s ease',
          animation: 'cardIn 0.5s ease-out 0.15s both',
        }}>
          {isLoading ? (
            <div style={{ padding: '3rem', textAlign: 'center' }}>
              <div style={{
                display: 'inline-block', width: 32, height: 32,
                border: '3px solid rgba(249,115,22,0.12)',
                borderTopColor: '#F97316', borderRadius: '50%',
                animation: 'categorySpin 0.7s linear infinite',
              }} />
              <div style={{ marginTop: '1rem', fontSize: '0.87rem', color: '#9B9590' }}>加载分类数据...</div>
            </div>
          ) : filteredTree.length === 0 ? (
            <div style={{ padding: '3rem', textAlign: 'center' }}>
              <div style={{
                width: 56, height: 56, borderRadius: 16,
                background: 'rgba(249,115,22,0.06)',
                display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                marginBottom: '1rem',
              }}>
                <svg width="28" height="28" viewBox="0 0 24 24" fill="none" stroke="#9B9590" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round">
                  <path d="M4 7V4h16v3" />
                  <path d="M9 20h6" />
                  <path d="M12 4v16" />
                </svg>
              </div>
              <div style={{ fontSize: '0.95rem', fontWeight: 600, color: '#6B6460', marginBottom: '0.3rem' }}>
                暂无分类数据
              </div>
              <p style={{ fontSize: '0.82rem', color: '#B5AEA8' }}>
                {searchInput || statusFilter ? '尝试调整筛选条件' : '点击右上角「添加分类」创建第一个分类'}
              </p>
            </div>
          ) : (
            <div>
              {filteredTree.map((node) => renderTreeNode(node, 0))}
            </div>
          )}
        </div>
      </div>

      {/* ===== Create Modal ===== */}
      {createOpen && createPortal(
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999 }}>
          <div
            role="button"
            tabIndex={0}
            style={{ position: 'fixed', inset: 0, background: 'rgba(42,37,32,0.45)', backdropFilter: 'blur(6px)', WebkitBackdropFilter: 'blur(6px)' }}
            onClick={() => !createMutation.isPending && setCreateOpen(false)}
            onKeyDown={(e) => e.key === 'Enter' && !createMutation.isPending && setCreateOpen(false)}
            aria-label="关闭"
          />
          <div style={{
            position: 'fixed', left: '50%', top: '50%', transform: 'translate(-50%, -50%)',
            width: 'calc(100% - 2rem)', maxWidth: 480,
            background: 'rgba(255,255,255,0.94)', backdropFilter: 'blur(24px)',
            WebkitBackdropFilter: 'blur(24px)',
            border: '1px solid rgba(255,255,255,0.7)', borderRadius: 24,
            boxShadow: '0 24px 64px rgba(42,37,32,0.18)',
            maxHeight: 'calc(100vh - 2rem)', display: 'flex', flexDirection: 'column', overflow: 'hidden',
          }}>
            {/* Header */}
            <div style={{ padding: '1.5rem 1.5rem 0' }}>
              <h3 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: '1.1rem', fontWeight: 700, color: '#2A2520',
                display: 'flex', alignItems: 'center', gap: '0.5rem',
              }}>
                <span style={{
                  width: 28, height: 28, borderRadius: 10,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  background: 'linear-gradient(135deg, #F97316, #FB923C)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5"><line x1="12" y1="5" x2="12" y2="19"/><line x1="5" y1="12" x2="19" y2="12"/></svg>
                </span>
                添加分类
              </h3>
            </div>

            {/* Body */}
            <div style={{ padding: '1.25rem 1.5rem', flex: 1, overflowY: 'auto' }}>
              {/* Name */}
              <div style={{ marginBottom: '1rem' }}>
                <label htmlFor="create-category-name" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  分类名称 <span style={{ color: '#E11D48' }}>*</span>
                </label>
                <input
                  id="create-category-name"
                  type="text"
                  value={createName}
                  onChange={(e) => setCreateName(e.target.value)}
                  placeholder="请输入分类名称"
                  maxLength={20}
                  style={{
                    width: '100%', padding: '0.65rem 0.85rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 12,
                    fontSize: '0.87rem', color: '#2A2520', outline: 'none',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = 'none'; }}
                />
              </div>

              {/* Parent */}
              <div style={{ marginBottom: '1rem' }}>
                <span style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  父级分类
                </span>
                <AdminSelect
                  options={parentOptions}
                  value={createParentId !== undefined ? String(createParentId) : ''}
                  onChange={(val) => setCreateParentId(val ? Number(val) : undefined)}
                />
                <p style={{ fontSize: '0.75rem', color: '#B5AEA8', marginTop: '0.25rem' }}>
                  不选则为一级分类，最多支持三级分类
                </p>
              </div>

              {/* Sort order */}
              <div style={{ marginBottom: '0.5rem' }}>
                <label htmlFor="create-category-sort" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  排序值
                </label>
                <input
                  id="create-category-sort"
                  type="number"
                  value={createSortOrder}
                  onChange={(e) => setCreateSortOrder(Number(e.target.value))}
                  min={0}
                  max={9999}
                  style={{
                    width: '100%', padding: '0.65rem 0.85rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 12,
                    fontSize: '0.87rem', color: '#2A2520', outline: 'none',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = 'none'; }}
                />
              </div>
            </div>

            {/* Footer */}
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.65rem',
              padding: '1rem 1.5rem',
              background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
              borderTop: '1px solid rgba(229,224,219,0.4)',
            }}>
              <button
                type="button"
                onClick={() => setCreateOpen(false)}
                disabled={createMutation.isPending}
                style={{
                  padding: '0.6rem 1.2rem', borderRadius: 12,
                  border: '1.5px solid #E5E0DB', background: '#fff',
                  fontSize: '0.87rem', fontWeight: 600, color: '#6B6460',
                  cursor: 'pointer', transition: 'all 0.15s ease',
                }}
              >
                取消
              </button>
              <button
                type="button"
                onClick={handleCreate}
                disabled={createMutation.isPending || !createName.trim()}
                style={{
                  padding: '0.6rem 1.5rem', borderRadius: 12, border: 'none',
                  background: createMutation.isPending || !createName.trim() ? '#D6CEC5' : 'linear-gradient(135deg, #F97316, #EA580C)',
                  fontSize: '0.87rem', fontWeight: 600, color: '#fff',
                  cursor: createMutation.isPending || !createName.trim() ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: createMutation.isPending || !createName.trim() ? 'none' : '0 3px 12px rgba(249,115,22,0.28)',
                }}
              >
                {createMutation.isPending ? '创建中...' : '确认创建'}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* ===== Edit Modal ===== */}
      {editOpen && editId && createPortal(
        <div style={{ position: 'fixed', inset: 0, zIndex: 9999 }}>
          <div
            role="button"
            tabIndex={0}
            style={{ position: 'fixed', inset: 0, background: 'rgba(42,37,32,0.45)', backdropFilter: 'blur(6px)', WebkitBackdropFilter: 'blur(6px)' }}
            onClick={() => !updateMutation.isPending && setEditOpen(false)}
            onKeyDown={(e) => e.key === 'Enter' && !updateMutation.isPending && setEditOpen(false)}
            aria-label="关闭"
          />
          <div style={{
            position: 'fixed', left: '50%', top: '50%', transform: 'translate(-50%, -50%)',
            width: 'calc(100% - 2rem)', maxWidth: 480,
            background: 'rgba(255,255,255,0.94)', backdropFilter: 'blur(24px)',
            WebkitBackdropFilter: 'blur(24px)',
            border: '1px solid rgba(255,255,255,0.7)', borderRadius: 24,
            boxShadow: '0 24px 64px rgba(42,37,32,0.18)',
            maxHeight: 'calc(100vh - 2rem)', display: 'flex', flexDirection: 'column', overflow: 'hidden',
          }}>
            {/* Header */}
            <div style={{ padding: '1.5rem 1.5rem 0' }}>
              <h3 style={{
                fontFamily: "'Playfair Display', 'Noto Serif SC', serif",
                fontSize: '1.1rem', fontWeight: 700, color: '#2A2520',
                display: 'flex', alignItems: 'center', gap: '0.5rem',
              }}>
                <span style={{
                  width: 28, height: 28, borderRadius: 10,
                  display: 'inline-flex', alignItems: 'center', justifyContent: 'center',
                  background: 'linear-gradient(135deg, #3B82F6, #2563EB)',
                  color: '#fff', flexShrink: 0,
                }}>
                  <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" strokeWidth="2.5">
                    <path d="M11 4H4a2 2 0 00-2 2v14a2 2 0 002 2h14a2 2 0 002-2v-7" />
                    <path d="M18.5 2.5a2.121 2.121 0 013 3L12 15l-4 1 1-4 9.5-9.5z" />
                  </svg>
                </span>
                编辑分类
              </h3>
            </div>

            {/* Body */}
            <div style={{ padding: '1.25rem 1.5rem', flex: 1, overflowY: 'auto' }}>
              {/* Name */}
              <div style={{ marginBottom: '1rem' }}>
                <label htmlFor="edit-category-name" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  分类名称 <span style={{ color: '#E11D48' }}>*</span>
                </label>
                <input
                  id="edit-category-name"
                  type="text"
                  value={editName}
                  onChange={(e) => setEditName(e.target.value)}
                  placeholder="请输入分类名称"
                  maxLength={20}
                  style={{
                    width: '100%', padding: '0.65rem 0.85rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 12,
                    fontSize: '0.87rem', color: '#2A2520', outline: 'none',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = 'none'; }}
                />
              </div>

              {/* Parent */}
              <div style={{ marginBottom: '1rem' }}>
                <span style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  父级分类
                </span>
                <AdminSelect
                  options={parentOptions}
                  value={editParentId !== undefined ? String(editParentId) : ''}
                  onChange={(val) => setEditParentId(val ? Number(val) : undefined)}
                />
              </div>

              {/* Sort order */}
              <div style={{ marginBottom: '1rem' }}>
                <label htmlFor="edit-category-sort" style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  排序值
                </label>
                <input
                  id="edit-category-sort"
                  type="number"
                  value={editSortOrder}
                  onChange={(e) => setEditSortOrder(Number(e.target.value))}
                  min={0}
                  max={9999}
                  style={{
                    width: '100%', padding: '0.65rem 0.85rem',
                    border: '1.5px solid #E5E0DB', borderRadius: 12,
                    fontSize: '0.87rem', color: '#2A2520', outline: 'none',
                    transition: 'all 0.2s ease',
                  }}
                  onFocus={(e) => { e.currentTarget.style.borderColor = '#F97316'; e.currentTarget.style.boxShadow = '0 0 0 3px rgba(249,115,22,0.08)'; }}
                  onBlur={(e) => { e.currentTarget.style.borderColor = '#E5E0DB'; e.currentTarget.style.boxShadow = 'none'; }}
                />
              </div>

              {/* Status */}
              <div>
                <span style={{ display: 'block', fontSize: '0.82rem', fontWeight: 600, color: '#2A2520', marginBottom: '0.4rem' }}>
                  状态
                </span>
                <div style={{ display: 'flex', gap: '0.75rem' }}>
                  <label style={{
                    display: 'flex', alignItems: 'center', gap: '0.4rem',
                    padding: '0.5rem 1rem', borderRadius: 10,
                    border: `1.5px solid ${editStatus === 1 ? '#10B981' : '#E5E0DB'}`,
                    background: editStatus === 1 ? 'rgba(16,185,129,0.05)' : '#fff',
                    cursor: 'pointer', transition: 'all 0.15s ease',
                  }}>
                    <input
                      type="radio"
                      checked={editStatus === 1}
                      onChange={() => setEditStatus(1)}
                      style={{ accentColor: '#10B981' }}
                    />
                    <span style={{ fontSize: '0.84rem', fontWeight: 500, color: editStatus === 1 ? '#059669' : '#6B6460' }}>启用</span>
                  </label>
                  <label style={{
                    display: 'flex', alignItems: 'center', gap: '0.4rem',
                    padding: '0.5rem 1rem', borderRadius: 10,
                    border: `1.5px solid ${editStatus === 0 ? '#F43F5E' : '#E5E0DB'}`,
                    background: editStatus === 0 ? 'rgba(244,63,94,0.05)' : '#fff',
                    cursor: 'pointer', transition: 'all 0.15s ease',
                  }}>
                    <input
                      type="radio"
                      checked={editStatus === 0}
                      onChange={() => setEditStatus(0)}
                      style={{ accentColor: '#F43F5E' }}
                    />
                    <span style={{ fontSize: '0.84rem', fontWeight: 500, color: editStatus === 0 ? '#E11D48' : '#6B6460' }}>禁用</span>
                  </label>
                </div>
              </div>
            </div>

            {/* Footer */}
            <div style={{
              display: 'flex', alignItems: 'center', justifyContent: 'flex-end', gap: '0.65rem',
              padding: '1rem 1.5rem',
              background: 'linear-gradient(180deg, rgba(250,248,245,0.5), rgba(250,248,245,0.9))',
              borderTop: '1px solid rgba(229,224,219,0.4)',
            }}>
              <button
                type="button"
                onClick={() => setEditOpen(false)}
                disabled={updateMutation.isPending}
                style={{
                  padding: '0.6rem 1.2rem', borderRadius: 12,
                  border: '1.5px solid #E5E0DB', background: '#fff',
                  fontSize: '0.87rem', fontWeight: 600, color: '#6B6460',
                  cursor: 'pointer', transition: 'all 0.15s ease',
                }}
              >
                取消
              </button>
              <button
                type="button"
                onClick={handleEdit}
                disabled={updateMutation.isPending || !editName.trim()}
                style={{
                  padding: '0.6rem 1.5rem', borderRadius: 12, border: 'none',
                  background: updateMutation.isPending || !editName.trim() ? '#D6CEC5' : 'linear-gradient(135deg, #3B82F6, #2563EB)',
                  fontSize: '0.87rem', fontWeight: 600, color: '#fff',
                  cursor: updateMutation.isPending || !editName.trim() ? 'not-allowed' : 'pointer',
                  transition: 'all 0.2s ease',
                  boxShadow: updateMutation.isPending || !editName.trim() ? 'none' : '0 3px 12px rgba(59,130,246,0.28)',
                }}
              >
                {updateMutation.isPending ? '保存中...' : '保存修改'}
              </button>
            </div>
          </div>
        </div>,
        document.body
      )}

      {/* ===== Delete Confirm ===== */}
      <ConfirmModal
        open={deleteTarget !== null}
        title="删除分类"
        content={deleteTarget ? `确定要删除分类「${deleteTarget.name}」吗？如果该分类下有子分类或关联商品，将无法删除。` : ''}
        confirmText="删除"
        cancelText="取消"
        variant="danger"
        loading={deleteMutation.isPending}
        onConfirm={handleDelete}
        onCancel={() => setDeleteTarget(null)}
      />

      {/* Animations */}
      <style>{`
        @keyframes pageIn { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes headerSlide { from { opacity: 0; transform: translateY(16px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes toolbarIn { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }
        @keyframes cardIn { from { opacity: 0; transform: translateY(16px) scale(0.99); } to { opacity: 1; transform: translateY(0) scale(1); } }
        @keyframes categorySpin { to { transform: rotate(360deg); } }
      `}</style>
    </div>
  );
}
