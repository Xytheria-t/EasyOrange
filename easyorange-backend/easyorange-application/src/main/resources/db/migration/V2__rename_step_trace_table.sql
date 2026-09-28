-- 步级轨迹表改名：eo_agent_step_trace → eo_tool_call_step_trace
--
-- 与编排器改名同批（AgentLoopRunner → ToolCallLoop 一路改到 ToolCallStep*）：
-- 落表的是「工具调用循环每一步」，不是某个叫 Agent 的东西的步骤，表名跟着代码走。
-- 列名与语义一个没动，只是换名 —— 平均步数 / 降级率 / 步级延迟三个口径的取数 SQL 无需改。
--
-- V1 不动：已部署的 V 版本禁止修改，改名走新增迁移（dev/it profile 关闭 validate，
-- 但改 V1 会让已初始化过的库在 validate 打开的环境直接启动失败）。
--
-- 索引名一并改：MySQL 的 RENAME TABLE 不动索引名，留 idx_eo_agent_step_trace_* 会与表名脱节。

RENAME TABLE `eo_agent_step_trace` TO `eo_tool_call_step_trace`;

ALTER TABLE `eo_tool_call_step_trace`
    RENAME INDEX `idx_eo_agent_step_trace_trace_id` TO `idx_eo_tool_call_step_trace_trace_id`,
    RENAME INDEX `idx_eo_agent_step_trace_tool_time` TO `idx_eo_tool_call_step_trace_tool_time`;
