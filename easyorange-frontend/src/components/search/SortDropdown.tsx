import { useState, useRef, useEffect } from 'react';
import { ChevronDown } from 'lucide-react';

export type SortOption = 'newest' | 'price_asc' | 'price_desc' | 'popular';

interface SortDropdownProps {
  value: SortOption;
  onChange: (value: SortOption) => void;
}

const SORT_OPTIONS: { value: SortOption; label: string }[] = [
  { value: 'newest', label: '最新发布' },
  { value: 'price_asc', label: '价格从低到高' },
  { value: 'price_desc', label: '价格从高到低' },
  { value: 'popular', label: '最受欢迎' },
];

export default function SortDropdown({ value, onChange }: SortDropdownProps) {
  const [open, setOpen] = useState(false);
  const ref = useRef<HTMLDivElement>(null);

  useEffect(() => {
    function handleClickOutside(e: MouseEvent) {
      if (ref.current && !ref.current.contains(e.target as Node)) {
        setOpen(false);
      }
    }
    document.addEventListener('mousedown', handleClickOutside);
    return () => document.removeEventListener('mousedown', handleClickOutside);
  }, []);

  const selectedLabel = SORT_OPTIONS.find(o => o.value === value)?.label ?? '排序方式';

  return (
    <div ref={ref} className="sort-dropdown relative">
      <button
        type="button"
        onClick={() => setOpen(!open)}
        className="sort-dropdown-trigger"
        aria-expanded={open}
        aria-haspopup="listbox"
      >
        <span>{selectedLabel}</span>
        <ChevronDown size={14} className={`sort-dropdown-arrow ${open ? 'rotate-180' : ''}`} />
      </button>
      {open && (
        <div className="sort-dropdown-panel" role="listbox">
          {SORT_OPTIONS.map(option => (
            <button
              key={option.value}
              type="button"
              role="option"
              aria-selected={value === option.value}
              className={`sort-dropdown-item ${value === option.value ? 'active' : ''}`}
              onClick={() => {
                onChange(option.value);
                setOpen(false);
              }}
            >
              <span>{option.label}</span>
              {value === option.value && (
                <span className="sort-dropdown-check">✓</span>
              )}
            </button>
          ))}
        </div>
      )}
    </div>
  );
}
