package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ReindexService;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台运维", description = "搜索索引重建")
@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@ConditionalOnBean(ReindexService.class)
public class AdminSearchReindexController {

    private final ReindexService reindexService;

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(reindexService.reindexAll());
    }
}
