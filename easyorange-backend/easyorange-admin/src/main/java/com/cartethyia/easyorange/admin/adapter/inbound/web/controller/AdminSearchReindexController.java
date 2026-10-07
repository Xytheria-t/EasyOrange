package com.cartethyia.easyorange.admin.adapter.inbound.web.controller;

import com.cartethyia.easyorange.admin.domain.port.AdminSearchIndexPort;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "管理后台-搜索索引", description = "搜索索引重建")
@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@ConditionalOnProperty(name = "easyorange.search.elasticsearch.enabled", havingValue = "true")
public class AdminSearchReindexController {

    private final AdminSearchIndexPort searchIndexPort;

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        return Result.success(searchIndexPort.reindexAll());
    }
}
