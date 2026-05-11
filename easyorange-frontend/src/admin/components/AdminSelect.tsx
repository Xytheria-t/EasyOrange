import { useState, useRef, useEffect, useCallback } from 'react';
import { createPortal } from 'react-dom';

export interface AdminSelectOption {
  value: string | number;
  label: string;
}

interface AdminSelectProps {
  options: AdminSelectOption[];
  value: string | number | undefined;
  onChange: (value: string) => void;
  placeholder?: string;
  minWidth?: number | string;
  style?: React.CSSProperties;
}

const CHEVRON_SVG = (
  <svg width="11" height="11" viewBox="0 0 24 24" fill="none" stroke="#8B857E" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
    <path d="m6 9 6 6 6-6" />
  </svg>
);

export function AdminSelect({
  options,
  value,
  onChange,
  placeholder = '',
  minWidth,
  style,
}: AdminSelectProps) {
  const [open, setOpen] = useState(false);
  const [pos, setPos] = useState({ top: 0, left: 0, width: 0 });
  const ref = useRef<HTMLDivElement>(null);
  const btnRef = useRef<HTMLButtonElement>(null);
  const listRef = useRef<HTMLDivElement>(null);

  const selectedLabel = options.find((o) => String(o.value) === String(value))?.label || placeholder || '';

  useEffect(() => {
    if (open && btnRef.current) {
      const rect = btnRef.current.getBoundingClientRect();
      setPos({
        top: rect.bottom + 4,
        left: rect.left,
        width: rect.width,
      });
    }
  }, [open]);

  useEffect(() => {
    const handleScroll = () => {
      if (open && btnRef.current) {
        const rect = btnRef.current.getBoundingClientRect();
        setPos({ top: rect.bottom + 4, left: rect.left, width: rect.width });
      }
    };
    window.addEventListener('scroll', handleScroll, true);
    return () => window.removeEventListener('scroll', handleScroll, true);
  }, [open]);

  const handleClickOutside = useCallback(
    (e: MouseEvent) => {
      const target = e.target as Node;
      if (
        ref.current && !ref.current.contains(target) &&
        listRef.current && !listRef.current.contains(target)
      ) {
        setOpen(false);
      }
    },
    [],
  );

  useEffect(() => {
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, [handleClickOutside]);

  useEffect(() => {
    if (open && listRef.current) {
      const selectedIdx = options.findIndex((o) => String(o.value) === String(value));
      if (selectedIdx >= 0 && listRef.current.children[selectedIdx]) {
        (listRef.current.children[selectedIdx] as HTMLElement).scrollIntoView({ block: 'nearest' });
      }
    }
  }, [open, value, options]);

  const handleSelect = (optValue: string | number) => {
    onChange(String(optValue));
    setOpen(false);
  };

  const panel = open ? createPortal(
    <div
      ref={listRef}
      style={{
        position: 'fixed',
        top: pos.top,
        left: pos.left,
        width: pos.width,
        background: 'rgba(255,255,255,0.92)',
        backdropFilter: 'blur(16px)',
        WebkitBackdropFilter: 'blur(16px)',
        border: '1px solid rgba(255,255,255,0.65)',
        borderRadius: 14,
        boxShadow: '0 12px 40px rgba(42,37,32,0.12), 0 4px 12px rgba(0,0,0,0.04)',
        zIndex: 99999,
        animation: 'adminSelectDropIn 0.18s cubic-bezier(0.16, 1, 0.3, 1)',
        maxHeight: 240,
        overflowY: 'auto',
      }}
    >
      {options.map((opt) => {
        const isActive = String(opt.value) === String(value);
        return (
          <button
            key={opt.value}
            type="button"
            onClick={(e) => { e.preventDefault(); handleSelect(opt.value); }}
            onMouseEnter={(e) => {
              if (!isActive) e.currentTarget.style.background = 'rgba(249,115,22,0.05)';
            }}
            onMouseLeave={(e) => {
              if (!isActive) e.currentTarget.style.background = 'transparent';
            }}
            style={{
              width: '100%', padding: '0.55rem 0.85rem',
              fontSize: '0.84rem', fontWeight: isActive ? 600 : 500,
              color: isActive ? '#EA580C' : '#4A4540',
              background: isActive ? 'linear-gradient(135deg, rgba(249,115,22,0.08), rgba(251,113,133,0.04))' : 'transparent',
              border: 'none', cursor: 'pointer',
              textAlign: 'left',
              transition: 'background 0.12s ease',
              display: 'flex', alignItems: 'center', justifyContent: 'space-between',
              gap: '0.5rem',
            }}
          >
            <span>{opt.label}</span>
            {isActive && (
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#F97316" strokeWidth="2.5" strokeLinecap="round" strokeLinejoin="round">
                <path d="M20 6L9 17l-5-5" />
              </svg>
            )}
          </button>
        );
      })}
    </div>,
    document.body,
  ) : null;

  return (
    <>
      <div
        ref={ref}
        style={{
          position: 'relative', display: 'inline-block', minWidth: minWidth ?? 110,
          ...style,
        }}
      >
        <button
          ref={btnRef}
          type="button"
          onClick={() => setOpen(!open)}
          style={{
            width: '100%',
            padding: '0.48rem 2rem 0.48rem 0.75rem',
            fontSize: '0.84rem', fontWeight: 500, color: '#2A2520',
            border: open ? '1.5px solid #F97316' : '1.5px solid #E5E0DB',
            borderRadius: 10, background: '#fff',
            cursor: 'pointer',
            textAlign: 'left', whiteSpace: 'nowrap',
            outline: 'none',
            transition: 'all 0.2s ease',
            boxShadow: open ? '0 0 0 3px rgba(249,115,22,0.08)' : 'none',
            display: 'flex', alignItems: 'center', justifyContent: 'space-between', gap: '0.4rem',
          }}
        >
          <span>{selectedLabel}</span>
          <span style={{ display: 'flex', alignItems: 'center', transform: open ? 'rotate(180deg)' : 'rotate(0deg)', transition: 'transform 0.2s ease' }}>
            {CHEVRON_SVG}
          </span>
        </button>
      </div>

      {panel}

      <style>{`
        @keyframes adminSelectDropIn {
          from { opacity: 0; transform: translateY(-6px) scale(0.97); }
          to { opacity: 1; transform: translateY(0) scale(1); }
        }
      `}</style>
    </>
  );
}
