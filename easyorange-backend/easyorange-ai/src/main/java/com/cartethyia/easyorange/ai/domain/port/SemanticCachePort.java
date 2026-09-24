package com.cartethyia.easyorange.ai.domain.port;

import com.cartethyia.easyorange.ai.domain.constant.AiCallScope;
import java.util.List;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * 语义缓存端口 — 按 scope + 查询文本缓存 AI 响应，近似问题命中即跳过模型调用。
 * <p>
 * 拆成三步而不是 {@code get}/{@code put} 两步，是为了让<b>一次查询只算一次向量</b>：
 * 查询向量化走供应商接口（按次计费 + 秒级延迟），若 get 与 put 各自向量化，
 * 未命中的那次请求要对同一个问题算两遍。调用方拿住 {@link #embedQuery} 的返回值，
 * 原样传给 {@link #lookUp} 与 {@link #store}。
 * <p>
 * <b>降级约定</b>：{@link #embedQuery} 在任何一步不可用时（缓存开关关闭 / embedding 模型未配置 /
 * 向量化异常）返回<b>空列表</b>，{@link #lookUp} 与 {@link #store} 收到空列表即不动作。
 * 于是「缓存不可用」在调用方眼里只有一种表现：没拿到 embedding → 问一次没命中 → 正常走 LLM。
 */
public interface SemanticCachePort {

    /**
     * 查询向量化。
     *
     * @return 查询向量；缓存不可用或向量化失败时返回空列表（调用方据此跳过缓存）
     */
    List<Float> embedQuery(String query);

    /**
     * 按余弦相似度查最近的历史回答。
     * <p>
     * <b>{@code userId} 是缓存键的一部分，不是过滤条件</b>：回答里注入了该用户的长期画像与会话历史，
     * 不带用户维度的共享桶会把 A 的个性化答案返给 B。匿名会话统一落到同一个字面量桶 ——
     * 它们不注入任何画像，共享安全。
     *
     * @param userId        调用方身份；{@code null} 表示匿名（按单一匿名桶缓存）
     * @param queryEmbedding {@link #embedQuery} 的返回值，空列表直接未命中
     * @return 相似度超过阈值的缓存回答，否则 {@link Optional#empty()}
     */
    <T> Optional<T> lookUp(
            AiCallScope scope, @Nullable String userId, String query, List<Float> queryEmbedding, Class<T> type);

    /**
     * 写入缓存，条目超上限时淘汰最旧一条。
     *
     * @param userId        调用方身份；{@code null} 表示匿名（与 {@link #lookUp} 同一分桶口径）
     * @param queryEmbedding {@link #embedQuery} 的返回值，空列表直接跳过
     */
    void store(AiCallScope scope, @Nullable String userId, String query, List<Float> queryEmbedding, Object response);

    /**
     * 缓存分桶键 —— 登录用户用其 id，匿名收敛到单一字面量。
     * <p>
     * 供调用方拼 stale 缓存等自建缓存键时复用，保证与语义缓存同一分桶口径。
     */
    static String cacheUserKey(@Nullable String userId) {
        return userId == null || userId.isBlank() ? "anon" : userId;
    }
}
