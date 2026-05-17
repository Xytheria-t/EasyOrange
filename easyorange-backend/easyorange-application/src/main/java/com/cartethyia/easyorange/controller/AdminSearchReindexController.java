package com.cartethyia.easyorange.controller;

import com.cartethyia.easyorange.adapter.outbound.elasticsearch.ReindexService;
import com.cartethyia.easyorange.common.result.Result;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/search")
@RequiredArgsConstructor
@ConditionalOnBean(ReindexService.class)
public class AdminSearchReindexController {

    private final ReindexService reindexService;

    @PostMapping("/reindex")
    public Result<Integer> reindex() {
        int count = reindexService.reindexAll();
        return Result.success(count);
    }
}
