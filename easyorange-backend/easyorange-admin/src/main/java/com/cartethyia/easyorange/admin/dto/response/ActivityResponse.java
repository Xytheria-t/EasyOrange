package com.cartethyia.easyorange.admin.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ActivityResponse {

    private String time;

    private String text;

    private String type;
}