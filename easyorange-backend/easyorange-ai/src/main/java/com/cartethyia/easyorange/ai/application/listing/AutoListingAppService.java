package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.dto.AutoListingResult;
import com.cartethyia.easyorange.ai.application.support.AiModelRouter;
import com.cartethyia.easyorange.ai.application.support.AiModelSupport;
import com.cartethyia.easyorange.ai.application.support.ToolCallDecider;
import com.cartethyia.easyorange.ai.application.support.ToolCallDecision;
import com.cartethyia.easyorange.ai.application.support.ToolLoopDecider;
import com.cartethyia.easyorange.ai.application.support.ToolLoopKernel;
import com.cartethyia.easyorange.ai.config.AiProperties;
import com.cartethyia.easyorange.ai.domain.annotation.TokenBudget;
import com.cartethyia.easyorange.ai.domain.enums.AiCallScope;
import com.cartethyia.easyorange.ai.domain.enums.AiResultCode;
import com.cartethyia.easyorange.ai.domain.model.TokenBudgetPolicy;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import com.cartethyia.easyorange.ai.domain.port.ChatStreamHandler;
import com.cartethyia.easyorange.ai.domain.port.PromptRegistryPort;
import com.cartethyia.easyorange.ai.domain.port.TokenBudgetStorePort;
import com.cartethyia.easyorange.common.exception.BusinessException;
import com.cartethyia.easyorange.product.domain.enums.ConditionLevel;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.support.ToolCallbacks;
import org.springframework.stereotype.Service;

/**
 * 发布助手 — 卖家侧的自治链路：视觉预识别 → 多步工具循环（查禁售 / 召回同类 / 行情定价）→ 生成上架表单。
 * <p>
 * 为什么这里是循环而买家侧找货可以「一次检索定结果」：发布任务每一步的输入依赖上一步的输出——品名决定
 * 查什么禁售规则、品类决定召回什么同类、行情统计必须在召回之后，「价格填错了」只有查完行情才能发现。
 * 编排内核与买家侧共用（{@link ToolLoopKernel}），注入的只有首轮上下文与降级口径。
 * <p>
 * 图片进轮的方式：决策轮是纯文本快模型，图片先经一次多模态调用转成结构化线索（{@link ImageClues}）进
 * 首轮上下文；最终生成带原图调视觉模型——「只写图片中可见的内容」靠看真图，不吃二手转述。
 * <p>
 * 失败口径：识别与生成失败一律显式报错（{@link AiResultCode#AI_UNAVAILABLE}），没有「部分可用」的表单；
 * 循环内的降级（步数 / 预算超限、决策失败）不报错——已积累的观察照常进生成，价格缺失由生成器置 null
 * 留给卖家。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AutoListingAppService {

    private static final String GENERATOR_PROMPT = "auto_listing";
    private static final String CLUE_PROMPT = "auto_listing_image_clues";

    private static final String CLUE_REQUEST_TEXT = "识别图片中的待售物品，返回结构化线索。";

    /** 与 {@code @TokenBudget(scenario = "auto_listing")} 注解一致：配置缺失时循环中途的预算判定兜到同一组值。 */
    static final int ANNOTATION_MAX_TOKENS_PER_CALL = 3000;

    static final int ANNOTATION_DAILY_TOKEN_LIMIT = 500_000;

    private final AiModelRouter modelRouter;
    private final PromptRegistryPort promptRegistry;
    private final AiModelSupport aiModelSupport;
    private final CategoryCatalogPort categoryCatalogPort;
    private final VisionImageLoader visionImageLoader;
    private final ToolLoopKernel loopKernel;
    private final ListingToolsFactory toolsFactory;
    private final ToolCallDecider decider;
    private final TokenBudgetStorePort budgetStore;
    private final AiProperties aiProperties;
    private final ListingLoopMetrics metrics;

    /** 非流式入口（无步骤事件）。 */
    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(List<String> imageUrls) {
        return analyzeImages(imageUrls, null, null);
    }

    /**
     * 拍照识别全链路 — {@code handler} 非空时逐步推送工具步事件（SSE 可视化）。
     * <p>
     * 流式路径的预算前置靠本注解的切面检查：预算异常在代理边界抛出，由流式 Controller 转 error 事件
     * （chat 那套手动预留是因为它要在方法体内做会话锁与半截回答落盘，发布链路没有这层需要转发的事件）。
     */
    @TokenBudget(scenario = "auto_listing", maxTokensPerCall = 3000, dailyTokenLimit = 500_000)
    public AutoListingResult analyzeImages(
            List<String> imageUrls, @Nullable String attributedUserId, @Nullable ChatStreamHandler handler) {
        // 模板缺失是部署期配置错误，require 在最前：静默报「AI 暂时不可用」会把「prompt 没打进包」藏起来
        String cluePrompt = promptRegistry.require(CLUE_PROMPT);
        String generatorPrompt = promptRegistry.require(GENERATOR_PROMPT);

        List<String> dataUrls;
        ImageClues clues;
        try {
            // 供应商抓不到 localhost/相对地址：先在服务端把图取回转 base64 data URL 再进模型调用；
            // 任一失败即整体失败（部分缺图的识别结果不可信）
            dataUrls = visionImageLoader.toDataUrls(imageUrls);
            clues = recognizeClues(cluePrompt, dataUrls);
        } catch (Exception e) {
            log.error("action=auto_listing_clue_failed, images={}", imageUrls.size(), e);
            throw BusinessException.of(AiResultCode.AI_UNAVAILABLE);
        }

        ListingLoopResult loop = runToolLoop(clues, null, null, attributedUserId, handler);

        try {
            return generateListing(generatorPrompt, loop, dataUrls);
        } catch (Exception e) {
            log.error("action=auto_listing_generate_failed, images={}", imageUrls.size(), e);
            throw BusinessException.of(AiResultCode.AI_UNAVAILABLE);
        }
    }

    /**
     * 只跑工具循环不生成 — 路由评估（金标准集 listing 用例）与生成解耦的入口，与买家侧「路由线只跑循环」
     * 同一口径。评估无图片：{@code sellerNote} 承载卖家线索文本，{@code clues} 传 null。
     */
    public ListingLoopResult runToolLoop(
            @Nullable ImageClues clues,
            @Nullable String sellerNote,
            @Nullable String sessionId,
            @Nullable String attributedUserId,
            @Nullable ChatStreamHandler handler) {
        ListingTools tools = toolsFactory.create();
        var spec = new ToolLoopKernel.Spec(
                promptRegistry.require("auto_listing_tool_system"),
                ListingPromptAssembler.firstDecisionMessage(clues, sellerNote, availableCategories()),
                List.of(ToolCallbacks.from(tools)),
                aiProperties.listing().maxSteps(),
                this::listingBudgetExhausted,
                // 决策失败不补检索：发布链路的失败态就是「行情缺失 → price 置 null 留卖家」，
                // 强塞一次猜测关键词的检索只会给生成喂噪声
                () -> {},
                sessionId,
                attributedUserId,
                handler,
                metrics);
        ToolLoopKernel.Outcome outcome = loopKernel.run(spec, listingDecider());
        return new ListingLoopResult(
                tools.knowledgeHits(), tools.assetHits(), outcome.outcome(), outcome.rounds(), outcome.toolPath());
    }

    /** 视觉预识别 — 循环前的一次多模态调用，产出决策轮可用的结构化线索；识别不出就显式失败，不给半张表。 */
    private ImageClues recognizeClues(String cluePrompt, List<String> dataUrls) {
        return aiModelSupport
                .callJsonAsWithImages(
                        modelRouter.choose(AiModelRouter.SCENARIO_VISION),
                        AiCallScope.AUTO_LISTING,
                        cluePrompt,
                        CLUE_REQUEST_TEXT,
                        dataUrls,
                        ImageClues.class)
                .orElseThrow(() -> BusinessException.of(AiResultCode.AI_UNAVAILABLE));
    }

    /**
     * 生成上架表单 — 带原图调视觉模型（描述质量取决于看真图），观察块与分类清单进 user 消息；行情统计
     * 缺失时由生成器按 prompt 硬约束把 price 置 null。
     */
    private AutoListingResult generateListing(String generatorPrompt, ListingLoopResult loop, List<String> dataUrls) {
        String userText =
                ListingPromptAssembler.generatorUserText(loop.knowledgeHits(), loop.assetHits(), availableCategories());
        // clues 只服务决策轮，不进生成消息：生成器 prompt 声明的上下文只有 <observations> 与 <category_options>，
        // 画面信息由随请求的原图承担
        Optional<AutoListingResult> listing = aiModelSupport.callJsonAsWithImages(
                modelRouter.choose(AiModelRouter.SCENARIO_VISION),
                AiCallScope.AUTO_LISTING,
                generatorPrompt,
                userText,
                dataUrls,
                AutoListingResult.class);
        if (listing.isEmpty()) {
            log.warn("action=auto_listing_empty_output, images={}, loop={}", dataUrls.size(), loop.outcome());
            throw BusinessException.of(AiResultCode.AI_UNAVAILABLE);
        }
        return sanitize(listing.get());
    }

    /**
     * 字段级净化 — 模型越界输出（负价格 / 清单外类目 / 非法成色码）置空交给用户填，不整单拒绝：标题描述
     * 大概率仍然可用，而脏值一旦随 ai_suggestion 快照落库，采纳率（唯一不靠 LLM 评 LLM 的质量数字）就被
     * 污染。json_object 只保证「是合法 JSON」，不保证「合业务约束」。
     */
    private AutoListingResult sanitize(AutoListingResult listing) {
        var available = Set.copyOf(categoryCatalogPort.listAvailableCategoryNames());
        var sanitized = new AutoListingResult(
                listing.title(),
                listing.description(),
                listing.price() != null && listing.price().signum() > 0 ? listing.price() : null,
                available.contains(listing.categoryName()) ? listing.categoryName() : null,
                isValidConditionLevel(listing.conditionLevel()) ? listing.conditionLevel() : null,
                listing.location());
        if (!sanitized.equals(listing)) {
            log.warn(
                    "action=auto_listing_sanitized, price={}, categoryName={}, conditionLevel={}",
                    listing.price(),
                    listing.categoryName(),
                    listing.conditionLevel());
        }
        return sanitized;
    }

    /** 成色码白名单复用 product 侧 {@link ConditionLevel}（code 1~4 的单一来源）；fromCode 对未知码 fail-fast，这里按「不合法」收敛。 */
    private static boolean isValidConditionLevel(@Nullable String value) {
        if (value == null) {
            return false;
        }
        try {
            ConditionLevel.fromCode(value);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }

    /** 类目清单现查现用 —— 一次小 SELECT，相对模型调用耗时可以忽略，清单改了下次识别就生效，省掉一层缓存失效策略。 */
    private List<String> availableCategories() {
        return categoryCatalogPort.listAvailableCategoryNames();
    }

    /** 决策走 chat_tool 场景快模型（决策是纯路由任务），记账进 auto_listing 场景预算——成本归业务链路、模型归职责。 */
    private ToolLoopDecider listingDecider() {
        return (sessionId, messages, callbacks) -> decider.decideForLoop(
                sessionId, messages, callbacks, AiCallScope.AUTO_LISTING, AutoListingAppService::toolInputOf);
    }

    /** 检索类工具取关键词做入参摘要，其余工具无摘要（与买家侧同口径）。 */
    private static String toolInputOf(ToolCallDecision decision) {
        return ListingTools.TOOL_KNOWLEDGE_SEARCH.equals(decision.tool())
                        || ListingTools.TOOL_PRODUCT_SEARCH.equals(decision.tool())
                ? decision.parsedArguments().query()
                : null;
    }

    /**
     * 循环中途的日预算判定 — 判定式单点在 {@link TokenBudgetPolicy}（全链路唯一），配置优先、注解兜底
     * （与 {@code ChatBudgetGuard} 同一口径，chat 的守卫类锁的是 chat 场景契约，这里按 auto_listing 场景判）。
     */
    private boolean listingBudgetExhausted() {
        int used = budgetStore
                .getTodayUsage(AiCallScope.AUTO_LISTING.budgetScenario())
                .map(TokenBudgetStorePort.TokenUsage::total)
                .orElse(0);
        var cfg = aiProperties.budget().resolve(AiCallScope.AUTO_LISTING.budgetScenario());
        int maxPerCall = cfg != null ? cfg.maxTokensPerCall() : ANNOTATION_MAX_TOKENS_PER_CALL;
        int dailyLimit = cfg != null ? cfg.dailyTokenLimit() : ANNOTATION_DAILY_TOKEN_LIMIT;
        return TokenBudgetPolicy.exhausted(used, maxPerCall, dailyLimit);
    }
}
