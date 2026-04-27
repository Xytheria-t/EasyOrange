package com.cartethyia.easyorange.product.application.command;

public class DeleteProductCommand {

    private Long id;

    public DeleteProductCommand() {
    }

    public DeleteProductCommand(Long id) {
        this.id = id;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private Long id;

        public Builder id(Long id) {
            this.id = id;
            return this;
        }

        public DeleteProductCommand build() {
            return new DeleteProductCommand(id);
        }
    }
}