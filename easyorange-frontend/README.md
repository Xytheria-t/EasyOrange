# EasyOrange 校园二手交易平台 - 前端

> 基于原生 TypeScript + Vite 构建的轻量级前端项目

## 📖 项目简介

EasyOrange 是一个面向校园用户的二手交易平台前端应用，采用纯 TypeScript 开发，无框架依赖，追求极致的性能和开发体验。

### ✨ 项目特色

- 🚀 **零框架依赖**: 原生 TypeScript，轻量快速
- 📦 **模块化设计**: 清晰的分层架构，易于维护
- 🎨 **响应式设计**: 完美适配移动端和桌面端
- ⚡ **Vite 构建**: 极速的开发体验和构建速度
- 🔒 **类型安全**: 完整的 TypeScript 类型定义
- 🎯 **性能优化**: 代码分割、资源懒加载

## 🛠️ 技术栈

### 核心技术
- **语言**: TypeScript 5.3+
- **构建工具**: Vite 5.0+
- **包管理器**: npm

### 开发工具
- **代码检查**: ESLint + TypeScript ESLint
- **代码格式化**: Prettier
- **类型检查**: TypeScript Compiler

### 运行时
- **零依赖**: 无第三方 UI 库和框架
- **原生 API**: 使用 Fetch API 进行网络请求
- **LocalStorage**: 本地数据持久化

## 📁 项目结构

```
easyorange-front/
├── src/                      # 源代码目录
│   ├── api/                  # API 接口层
│   │   ├── request.ts        # HTTP 请求封装
│   │   ├── product.ts        # 商品 API
│   │   ├── order.ts          # 订单 API
│   │   └── ...
│   ├── components/           # 可复用组件
│   │   ├── Header.ts         # 顶部导航
│   │   ├── ProductCard.ts    # 商品卡片
│   │   └── ...
│   ├── pages/                # 页面逻辑层
│   │   ├── index.ts          # 首页
│   │   ├── products.ts       # 商品列表
│   │   ├── profile.ts        # 个人中心
│   │   └── publish/          # 发布商品模块
│   ├── styles/               # 样式文件
│   │   ├── main.css          # 全局样式
│   │   └── ...
│   ├── types/                # 类型定义
│   │   └── ...
│   ├── utils/                # 工具函数
│   │   └── ...
│   └── main.ts               # 入口文件
├── index.html                # 首页入口
├── products.html             # 商品列表页
├── profile.html              # 个人中心页
├── publish.html              # 发布商品页
└── vite.config.ts            # Vite 配置
```

详细文件结构请查看：[项目文件结构.md](./项目文件结构.md)

## 🚀 快速开始

### 环境要求

- Node.js >= 16.0.0
- npm >= 8.0.0

### 安装依赖

```bash
npm install
```

### 启动开发服务器

```bash
# 方式一：使用启动脚本（Windows）
.\启动前端.ps1

# 方式二：手动启动
npm run dev
```

访问地址：http://localhost:3000/

### 生产构建

```bash
# 构建
npm run build

# 预览构建结果
npm run preview
```

## 📋 可用命令

| 命令 | 说明 |
|------|------|
| `npm run dev` | 启动开发服务器 (:3000) |
| `npm run build` | 生产构建 |
| `npm run build:js` | 仅构建 JavaScript |
| `npm run build:analyze` | 构建并分析包大小 |
| `npm run preview` | 预览构建结果 |
| `npm run lint` | 代码检查并自动修复 |
| `npm run lint:check` | 仅检查代码 |
| `npm run format` | 格式化代码 |
| `npm run format:check` | 仅检查格式 |
| `npm run typecheck` | TypeScript 类型检查 |
| `npm run clean` | 清理构建产物 |
| `npm run rebuild` | 清理并重新构建 |

## 🎯 功能模块

### 核心功能

#### 1. 商品模块
- ✅ 商品列表展示
- ✅ 商品搜索和筛选
- ✅ 商品详情查看
- ✅ 商品发布（支持图片上传）
- ✅ 商品编辑和删除

#### 2. 用户模块
- ✅ 用户注册和登录
- ✅ 个人资料管理
- ✅ 会员系统
- ✅ 收藏夹管理
- ✅ 关注系统

#### 3. 交易模块
- ✅ 订单创建
- ✅ 在线支付
- ✅ 订单管理
- ✅ 退款/售后
- ✅ 评价系统

#### 4. 互动模块
- ✅ 站内消息
- ✅ 实时聊天
- ✅ 系统通知
- ✅ 活动页面

### 页面列表

| 页面 | 路由 | 说明 |
|------|------|------|
| 首页 | `/` | 推荐商品、轮播图 |
| 商品列表 | `/products.html` | 商品搜索、筛选 |
| 个人中心 | `/profile.html` | 个人信息、订单管理 |
| 发布商品 | `/publish.html` | 商品发布表单 |
| 收藏夹 | `/favorites.html` | 收藏商品列表 |

## 📊 代码统计

### 文件统计
- **TypeScript 文件**: 35+ 个
- **CSS 样式文件**: 7 个
- **HTML 页面**: 5 个
- **API 接口**: 18 个模块
- **组件**: 5 个可复用组件

### 代码行数（估算）
- **业务逻辑**: ~3000 行
- **样式代码**: ~1500 行
- **类型定义**: ~500 行
- **总计**: ~5000 行

## 🎨 设计规范

### 颜色系统

```css
/* 主色 - 活力橙 */
--primary-color: #ff6b35;
--primary-light: #ff8c61;
--primary-dark: #e85a2d;

/* 辅助色 */
--success-color: #00c853;
--warning-color: #ff9800;
--danger-color: #ff5252;
--info-color: #5c6bc0;
```

### 响应式断点

```css
/* 移动端 */
@media (max-width: 768px) { }

/* 平板端 */
@media (min-width: 769px) and (max-width: 1024px) { }

/* 桌面端 */
@media (min-width: 1025px) { }
```

## 🔧 配置说明

### 环境变量

#### 开发环境 (`.env.development`)
```env
VITE_API_BASE_URL=/api
VITE_APP_TITLE=EasyOrange - 校园二手交易平台
VITE_API_TIMEOUT=10000
VITE_PAGE_SIZE=20
VITE_MAX_IMAGE_SIZE=5
VITE_IMAGE_QUALITY=0.8
VITE_AUTO_SAVE_INTERVAL=30
```

#### 生产环境 (`.env.production`)
```env
VITE_API_BASE_URL=https://api.easyorange.com
VITE_APP_TITLE=EasyOrange - 校园二手交易平台
VITE_API_TIMEOUT=10000
VITE_PAGE_SIZE=20
VITE_MAX_IMAGE_SIZE=5
VITE_IMAGE_QUALITY=0.75
VITE_AUTO_SAVE_INTERVAL=30
```

### Vite 配置要点

```typescript
// vite.config.ts
export default defineConfig({
  base: './',
  server: {
    port: 3000,
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true
      }
    }
  },
  build: {
    outDir: 'dist',
    minify: 'terser',
    rollupOptions: {
      input: {
        main: 'index.html',
        products: 'products.html',
        profile: 'profile.html',
        publish: 'publish.html',
        favorites: 'favorites.html'
      }
    }
  }
})
```

## 📝 开发规范

### 代码风格

#### TypeScript
- 使用 `camelCase` 命名变量和函数
- 使用 `PascalCase` 命名类型和接口
- 所有函数和变量必须有类型注解
- 优先使用 `const` 而非 `let`

#### CSS
- 使用 BEM 命名规范
- 优先使用 CSS 变量
- 避免使用 `!important`

#### 文件组织
- 相关功能放在同一目录
- 单一职责原则
- 文件不超过 300 行

### Git 提交规范

```bash
feat: 新功能
fix: 修复 bug
docs: 文档更新
style: 代码格式调整
refactor: 重构代码
test: 测试相关
chore: 构建/工具链相关
```

## 🚀 性能优化

### 已实施的优化

1. **代码分割**
   - 按页面分割代码
   - 第三方库单独打包

2. **资源优化**
   - 图片懒加载
   - CSS 压缩
   - JavaScript Tree Shaking

3. **缓存策略**
   - 静态资源长期缓存
   - API 响应本地缓存

4. **渲染优化**
   - 骨架屏加载
   - 图片懒加载（IntersectionObserver）
   - 防抖节流
   - DocumentFragment 批量 DOM 操作

### 性能指标

| 指标 | 目标值 | 说明 |
|------|--------|------|
| 首屏加载 | < 1.5s | 需实际测量 |
| LCP | < 2.5s | 需实际测量 |
| FID | < 100ms | 需实际测量 |
| CLS | < 0.1 | 需实际测量 |

> 注：以上为目标值，实际性能需通过 Lighthouse 等工具测量。

## 🔍 常见问题

### Q: 为什么选择原生 TypeScript 而不是框架？

**A**: 
- 项目规模适中，原生开发更轻量
- 完全控制代码结构和性能
- 学习 TypeScript 最佳实践
- 避免框架的过度设计

### Q: 如何添加新页面？

**A**:
1. 创建 HTML 文件（如 `new-page.html`），引入 `<script type="module" src="/src/main.ts"></script>`
2. 在 `src/pages/` 创建对应的 `.ts` 文件
3. 在 `.ts` 文件中监听 `DOMContentLoaded` 事件并初始化页面逻辑
4. 在 `vite.config.ts` 的 `rollupOptions.input` 中添加新页面入口

### Q: 如何调用新的 API？

**A**:
1. 在 `src/api/` 创建新的 API 模块
2. 使用 `request.ts` 中的封装方法
3. 定义好 TypeScript 类型
4. 在页面逻辑中导入使用

### Q: 如何调试代码？

**A**:
- 使用浏览器 DevTools 的 Source 面板
- 设置断点调试 TypeScript（需要 Source Map）
- 使用 `console.log` 输出调试信息
- 使用 ESLint 检查代码错误

## 📚 学习资源

### TypeScript
- [官方文档](https://www.typescriptlang.org/zh/)
- [TypeScript 入门教程](https://ts.xcatliu.com/)

### Vite
- [官方文档](https://cn.vitejs.dev/)
- [Vite 构建配置指南](https://cn.vitejs.dev/guide/build.html)

### 原生 JavaScript
- [MDN Web Docs](https://developer.mozilla.org/zh-CN/)
- [现代 JavaScript 教程](https://zh.javascript.info/)

## 🤝 贡献指南

欢迎贡献代码！请遵循以下步骤：

1. Fork 本项目
2. 创建特性分支 (`git checkout -b feature/AmazingFeature`)
3. 提交更改 (`git commit -m 'Add some AmazingFeature'`)
4. 推送到分支 (`git push origin feature/AmazingFeature`)
5. 提交 Pull Request

## 📄 许可证

MIT License

## 📞 联系方式

- 项目地址：https://gitee.com/cartethyia_XLS/easy-orange
- 问题反馈：提交 Issue
- 邮箱：support@easyorange.com

---

**最后更新**: 2024-03-07  
**当前版本**: 1.0.0  
**维护状态**: 🟢 活跃开发中
