package com.cartethyia.easyorange.framework.cache;

@FunctionalInterface
public interface CacheLoader<T> {

    T load();
}