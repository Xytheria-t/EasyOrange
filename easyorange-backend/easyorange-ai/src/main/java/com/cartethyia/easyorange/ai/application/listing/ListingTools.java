package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.application.support.RetrievalObservations;
import com.cartethyia.easyorange.ai.domain.model.AssetHit;
import com.cartethyia.easyorange.ai.domain.model.KnowledgeHit;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;

/**
 * 发布链路的工具面 — 5 个工具（规则检索 / 品类召回 / 行情统计 / 类目清单 / 收敛），每次循环实例化一份，
 * 召回物累加器是单次请求内的可变状态、由实例独占（与 {@code ChatTools} 同一并发约定）。
 * <p>
 * knowledge_search / product_search / market_price_stats / finish 与买家侧工具面<b>同名同义</b>（检索
 * 语义、观察文案、冗余判据全部同源）：golden-set 的 expected_tools、前端 STEP_LABELS、trace 的工具列
 * 三处只认一份词汇。list_categories 是发布链路独有的第 5 个工具——决策器 prompt 的分类约束锚在它身上。
 * <p>
 * 没有写路径：发布链路的一切产出都进最终生成的表单由卖家确认，没有 chat 链路 remember_preference 那类
 * 直接落库的工具。抛异常 = 该步失败；「查无 / 未命中」返回观察文本。
 */
@SuppressWarnings("unused") // thought 只进工具 schema，方法体不消费
public class ListingTools {

    /** 与 ChatTools 同名常量各写一份（跨能力包不互相引用），字面一致性由 ListingToolsTest 钉住。 */
    public static final String TOOL_KNOWLEDGE_SEARCH = "knowledge_search";

    public static final String TOOL_PRODUCT_SEARCH = "product_search";

    public static final String TOOL_MARKET_PRICE_STATS = "market_price_stats";

    public static final String TOOL_LIST_CATEGORIES = "list_categories";

    public static final String TOOL_FINISH = "finish";

    public static final Set<String> TOOL_NAMES = Set.of(
            TOOL_KNOWLEDGE_SEARCH, TOOL_PRODUCT_SEARCH, TOOL_MARKET_PRICE_STATS, TOOL_LIST_CATEGORIES, TOOL_FINISH);

    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final CategoryCatalogPort categoryCatalogPort;

    /** 各轮召回物累加器 — 请求内可变状态，实例独占持有；读取走只读快照方法，不交出可变引用。 */
    private final List<KnowledgeHit> knowledgeHits = new ArrayList<>();

    private final List<AssetHit> assetHits = new ArrayList<>();

    ListingTools(
            KnowledgeRetrievalAppService retrievalService,
            AssetSourcingAppService assetSourcingService,
            CategoryCatalogPort categoryCatalogPort) {
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.categoryCatalogPort = categoryCatalogPort;
    }

    List<KnowledgeHit> knowledgeHits() {
        return List.copyOf(knowledgeHits);
    }

    List<AssetHit> assetHits() {
        return List.copyOf(assetHits);
    }

    @Tool(
            name = TOOL_KNOWLEDGE_SEARCH,
            description = "检索平台规则知识库（禁售品类 / 发布限制 / 交易规则）；上架前判断「能不能卖、有什么限制」必须先查这里",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String searchKnowledge(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "检索关键词，3-10 字（品类 + 禁售 / 限制）") String query) {
        List<KnowledgeHit> found = retrievalService.search(query, RetrievalObservations.TOP_K);
        var turn = RetrievalObservations.knowledgeTurn(knowledgeHits, found);
        knowledgeHits.addAll(turn.fresh());
        return turn.observation();
    }

    @Tool(
            name = TOOL_PRODUCT_SEARCH,
            description = "按品类 / 品名召回在售资产，作为定价的同类参照",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String searchProducts(
            @ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought,
            @ToolParam(description = "检索关键词，3-10 字，保留品类与型号") String query) {
        List<AssetHit> found = assetSourcingService.search(query, RetrievalObservations.TOP_K);
        var turn = RetrievalObservations.assetTurn(assetHits, found);
        assetHits.addAll(turn.fresh());
        return turn.observation();
    }

    // 顺序约束写进 schema 描述而不只写 system prompt：模型是在选工具时读它的，等生成时才发现约束已经晚一轮
    @Tool(
            name = TOOL_MARKET_PRICE_STATS,
            description = "对已召回的在售资产算行情（在售件数 / 均价 / 价格区间），作为建议售价的依据。"
                    + "必须在 product_search 之后的轮次调用（同一轮里它会排在 product_search 前面而统计到空集）",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String summarizeMarketPrice(@ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought) {
        return RetrievalObservations.priceStatsObservation(assetHits);
    }

    @Tool(
            name = TOOL_LIST_CATEGORIES,
            description = "取平台可用分类清单；产出上架信息时 categoryName 必须原样取自这份清单",
            resultConverter = RetrievalObservations.ObservationTextConverter.class)
    public String listCategories(@ToolParam(description = "本步理由，不超过 20 字的中文概括") String thought) {
        List<String> categories = categoryCatalogPort.listAvailableCategoryNames();
        if (categories.isEmpty()) {
            return "平台暂无可用分类";
        }
        return "可用分类：" + String.join("、", categories);
    }

    @Tool(name = TOOL_FINISH, description = "合规结论与行情基准已足够产出上架信息，不再调用任何工具")
    public String finish(@ToolParam(description = "收敛理由，不超过 20 字的中文概括") String thought) {
        // 方法体永不执行，这里只为让 finish 出现在发给供应商的工具 schema 里
        return TOOL_FINISH;
    }
}
