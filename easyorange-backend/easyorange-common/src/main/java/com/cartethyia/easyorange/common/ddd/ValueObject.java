package com.cartethyia.easyorange.common.ddd;

public abstract class ValueObject {

    @Override
    public abstract boolean equals(Object obj);

    @Override
    public abstract int hashCode();

    protected boolean equalsValueObject(ValueObject other) {
        return this.getClass().equals(other.getClass());
    }
}
