package com.cartethyia.easyorange.product.application.command;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DeleteProductCommand {

    private Long id;
}