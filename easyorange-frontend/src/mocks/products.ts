/**
 * @fileoverview 模拟商品数据 - 用于开发环境测试
 */

export interface MockProduct {
  id: number;
  title: string;
  description: string;
  price: number;
  originalPrice: number | null;
  categoryId: number;
  categoryName: string;
  condition: 'NEW' | 'LIKE_NEW' | 'GOOD' | 'FAIR' | 'POOR';
  conditionName: string;
  status: string;
  images: string[];
  location: string;
  views: number;
  favorites: number;
  sellerId: number;
  sellerName: string;
  sellerAvatar: string | null;
  sellerRating: number;
  createTime: string;
  updateTime: string;
}

const MOCK_CATEGORIES = [
  { id: 1, name: '图书教材', icon: '📚' },
  { id: 2, name: '电子产品', icon: '💻' },
  { id: 3, name: '服装鞋包', icon: '👔' },
  { id: 4, name: '生活用品', icon: '🏠' },
  { id: 5, name: '运动户外', icon: '⚽' },
  { id: 6, name: '美妆护肤', icon: '💄' },
  { id: 7, name: '交通工具', icon: '🚲' },
  { id: 8, name: '其他', icon: '📦' },
];

const MOCK_CONDITIONS = [
  { value: 'NEW', name: '全新', icon: '✨' },
  { value: 'LIKE_NEW', name: '几乎全新', icon: '🌟' },
  { value: 'GOOD', name: '轻微使用', icon: '💫' },
  { value: 'FAIR', name: '明显使用', icon: '⭐' },
];

const MOCK_PRODUCT_TITLES = [
  '大学英语四级词汇书', 'MacBook Pro 2020', '耐克运动鞋', '宜家书桌',
  '高等数学教材', 'AirPods Pro', 'Adidas运动T恤', '小米空气净化器',
  '机械键盘', '显示器支架', '蓝牙音箱', '电动牙刷', '咖啡机', '登山背包', '瑜伽垫', '台灯',
  'iPad Air 4', 'Switch游戏机', '人体工学椅', 'Kindle电子书',
  '索尼降噪耳机', '富士拍立得', '健身哑铃', '保温杯',
];

const MOCK_LOCATIONS = ['图书馆', '食堂', '宿舍楼下', '教学楼', '南门', '北门'];

export function generateMockProducts(count: number): MockProduct[] {
  return Array.from({ length: count }, (_, i) => {
    const category = MOCK_CATEGORIES[Math.floor(Math.random() * MOCK_CATEGORIES.length)];
    const condition = MOCK_CONDITIONS[Math.floor(Math.random() * MOCK_CONDITIONS.length)];
    const price = Math.floor(Math.random() * 1000) + 10;
    const originalPrice = Math.random() > 0.5 ? price + Math.floor(Math.random() * 500) : null;
    const createdDate = new Date(Date.now() - Math.random() * 7 * 24 * 60 * 60 * 1000);
    const updatedDate = new Date(createdDate.getTime() + Math.random() * 2 * 24 * 60 * 60 * 1000);

    return {
      id: i + 1,
      title: MOCK_PRODUCT_TITLES[i % MOCK_PRODUCT_TITLES.length],
      description: '这是一件优质的二手商品，成色新，性价比极高。适合校园交易。',
      price,
      originalPrice,
      categoryId: category.id,
      categoryName: category.name,
      condition: condition.value as MockProduct['condition'],
      conditionName: condition.name,
      status: 'ONLINE',
      images: [
        `https://picsum.photos/seed/product${i}a/400/400`,
        `https://picsum.photos/seed/product${i}b/400/400`,
      ],
      location: MOCK_LOCATIONS[Math.floor(Math.random() * MOCK_LOCATIONS.length)],
      views: Math.floor(Math.random() * 500),
      favorites: Math.floor(Math.random() * 50),
      sellerId: Math.floor(Math.random() * 10) + 1,
      sellerName: `用户${Math.floor(Math.random() * 100)}`,
      sellerAvatar: null,
      sellerRating: Math.random() * 2 + 3,
      createTime: createdDate.toISOString(),
      updateTime: updatedDate.toISOString(),
    };
  });
}

export const mockCategories = MOCK_CATEGORIES;
export const mockConditions = MOCK_CONDITIONS;
