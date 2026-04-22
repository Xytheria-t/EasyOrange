package com.cartethyia.easyorange.common.ddd;

public interface ValueObject {
    boolean equals(Object obj);

    int hashCode();
}