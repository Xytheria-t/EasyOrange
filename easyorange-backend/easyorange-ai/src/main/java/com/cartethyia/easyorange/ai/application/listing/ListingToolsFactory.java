package com.cartethyia.easyorange.ai.application.listing;

import com.cartethyia.easyorange.ai.application.retrieval.AssetSourcingAppService;
import com.cartethyia.easyorange.ai.application.retrieval.KnowledgeRetrievalAppService;
import com.cartethyia.easyorange.ai.domain.port.CategoryCatalogPort;
import org.springframework.stereotype.Component;

/**
 * 发布链路工具面装配 — 一次循环用的 {@link ListingTools} 从这里实例化，编排器不持有工具面背后的三个协作方
 * （与买家侧 {@code ChatToolsFactory} 同一取舍：为一次 new 承担依赖清单，会让工具面演进与循环控制被迫同批）。
 * <p>
 * 只组装不加工；每次循环一份新实例是 {@link ListingTools} 自己的并发要求（召回累加器是实例独占的可变状态）。
 */
@Component
public class ListingToolsFactory {

    private final KnowledgeRetrievalAppService retrievalService;
    private final AssetSourcingAppService assetSourcingService;
    private final CategoryCatalogPort categoryCatalogPort;

    ListingToolsFactory(
            KnowledgeRetrievalAppService retrievalService,
            AssetSourcingAppService assetSourcingService,
            CategoryCatalogPort categoryCatalogPort) {
        this.retrievalService = retrievalService;
        this.assetSourcingService = assetSourcingService;
        this.categoryCatalogPort = categoryCatalogPort;
    }

    ListingTools create() {
        return new ListingTools(retrievalService, assetSourcingService, categoryCatalogPort);
    }
}
