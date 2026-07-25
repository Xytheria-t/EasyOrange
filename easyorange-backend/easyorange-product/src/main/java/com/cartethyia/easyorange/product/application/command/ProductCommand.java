package com.cartethyia.easyorange.product.application.command;

public sealed interface ProductCommand
    permits CreateProductCommand, UpdateProductCommand, CreateProductRatingCommand {
}
