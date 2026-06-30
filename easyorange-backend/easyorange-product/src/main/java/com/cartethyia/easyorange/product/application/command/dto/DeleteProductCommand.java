package com.cartethyia.easyorange.product.application.command.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeleteProductCommand {

    private String id;

    public static DeleteProductCommand from(String id) {
        return DeleteProductCommand.builder().id(id).build();
    }
}
