package com.cartethyia.easyorange.product.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RestoreStockCommand {

    private Long productId;
}
