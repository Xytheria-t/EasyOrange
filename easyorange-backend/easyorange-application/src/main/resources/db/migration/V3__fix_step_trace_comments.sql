-- 步级轨迹表注释对齐：V2 改了表名，COMMENT 还停在改名前的说法；
-- tool 列注释列了 4 个工具名，而工具面早已扩到 7 个（market_price_stats /
-- compare_assets / remember_preference 是后加的）。
--
-- 列注释不再罗列工具名字面量——那份清单随 ChatTools.TOOL_NAMES 变动，
-- 写死在 schema 里迟早又对不上；改成指向代码里的封闭集，谁漏改一眼能看出来。
-- 按 DATABASE.md 的约定「调用方可能按 COMMENT 判断」，注释必须准确。

ALTER TABLE `eo_tool_call_step_trace`
    MODIFY COLUMN `tool` VARCHAR(32) NOT NULL
        COMMENT '工具（7 个 @Tool 名，取值域见 ChatTools.TOOL_NAMES）',
    COMMENT = '工具调用循环步级轨迹（平均步数 / 降级率 / 步级延迟 p95 数据源）';
