package com.cartethyia.easyorange.admin.adapter.inbound.web.dto.response;

import java.util.List;

public record BatchHandleResultResponse(int total, int success, int failed, List<String> errors) {}
