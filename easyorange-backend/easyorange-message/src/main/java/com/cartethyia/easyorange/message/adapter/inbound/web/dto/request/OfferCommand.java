package com.cartethyia.easyorange.message.adapter.inbound.web.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * 买家出价命令
 *
 * @author cartethyia
 * @date 2026/06/24
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class OfferCommand {

    @NotNull
    private Long productId;

    @NotNull
    private BigDecimal offerPrice;
}
