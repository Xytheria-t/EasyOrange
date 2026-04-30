/**
 * @fileoverview 格式化工具模块
 * @description 提供日期、数字、字符串等格式化功能
 */

export function formatCurrency(amount: number, currency = 'CNY'): string {
  return new Intl.NumberFormat('zh-CN', {
    style: 'currency',
    currency
  }).format(amount);
}

export function formatPrice(price: number): string {
  return price.toFixed(2);
}

export function formatCondition(condition: string): string {
  const conditionMap: Record<string, string> = {
    'new': '全新',
    'like_new': '几乎全新',
    'good': '良好',
    'fair': '一般',
    'poor': '较差'
  };
  return conditionMap[condition] || condition;
}

export function formatDate(
  date: Date | string | number,
  format: 'date' | 'time' | 'datetime' = 'datetime'
): string {
  if (!date) {return '--';}
  
  try {
    const d = new Date(date);
    if (isNaN(d.getTime())) {return '--';}
    
    const options: Record<string, Intl.DateTimeFormatOptions> = {
      date: { year: 'numeric', month: '2-digit', day: '2-digit' },
      time: { hour: '2-digit', minute: '2-digit' },
      datetime: { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }
    };
    
    return d.toLocaleDateString('zh-CN', options[format] ?? options.datetime);
  } catch {
    return '--';
  }
}

export function formatRelativeTime(date: Date | string | number): string {
  if (!date) {return '--';}
  
  try {
    const d = new Date(date);
    const now = new Date();
    const diff = now.getTime() - d.getTime();
    const minutes = Math.floor(diff / 60000);
    const hours = Math.floor(minutes / 60);
    const days = Math.floor(hours / 24);

    if (minutes < 1) {return '刚刚';}
    if (minutes < 60) {return `${minutes}分钟前`;}
    if (hours < 24) {return `${hours}小时前`;}
    if (days < 7) {return `${days}天前`;}
    if (days < 30) {return `${Math.floor(days / 7)}周前`;}
    if (days < 365) {return `${Math.floor(days / 30)}个月前`;}
    return `${Math.floor(days / 365)}年前`;
  } catch {
    return '--';
  }
}

export function escapeHtml(str: string | null | undefined): string {
  if (str == null) {return '';}
  if (typeof str !== 'string') {str = String(str);}
  
  const HTML_ENTITIES: Record<string, string> = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;',
    '"': '&quot;',
    "'": '&#x27;',
    '/': '&#x2F;',
    '`': '&#x60;',
    '=': '&#x3D;'
  };
  
  return str.replace(/[&<>"'`=/]/g, (char) => HTML_ENTITIES[char]);
}

export function parseQueryString(queryString = window.location.search): Record<string, string> {
  const params = new URLSearchParams(queryString.replace(/^\?/, ''));
  const result: Record<string, string> = {};
  params.forEach((value, key) => {
    result[key] = value;
  });
  return result;
}

export function buildQueryString(params: Record<string, string | number | boolean | null | undefined>): string {
  const filtered = Object.entries(params)
    .filter(([, value]) => value !== null && value !== undefined && value !== '')
    .map(([key, value]) => [key, String(value)]);
  return new URLSearchParams(filtered).toString();
}
