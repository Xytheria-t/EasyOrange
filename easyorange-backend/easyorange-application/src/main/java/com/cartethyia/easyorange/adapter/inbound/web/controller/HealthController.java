package com.cartethyia.easyorange.adapter.inbound.web.controller;

import com.cartethyia.easyorange.adapter.inbound.web.response.HealthResponse;
import com.cartethyia.easyorange.common.result.Result;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "平台运维", description = "健康检查")
@RestController
@RequestMapping("/api")
public class HealthController {

    @GetMapping("/health")
    public Result<HealthResponse> health() {
        return Result.success(new HealthResponse("UP", Instant.now()));
    }
}
