import type { PageResult } from '@/types';

/**
 * 只暴露列表页真正消费的三个字段；`size` / `pages` 全仓无人使用，留在返回形状里
 * 只会让整包数据参与结构共享的深比较。
 *
 * 必须定义在模块作用域而非内联：query-core 对「select 引用未变 + data 未变」有
 * 复用快路径（queryObserver 的 #selectResult），内联会让每次渲染都重跑 select。
 */
export const selectList = <T>(data: PageResult<T>) => ({
    records: data.records,
    total: data.total,
    current: data.current,
});
