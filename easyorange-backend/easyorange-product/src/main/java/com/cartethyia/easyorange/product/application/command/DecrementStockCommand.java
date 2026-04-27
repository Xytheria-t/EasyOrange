package com.cartethyia.easyorange.product.application.command;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DecrementStockCommand {

    private Long productId;
}
