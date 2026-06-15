package com.cartethyia.easyorange.framework.bloom;

public interface BloomFilter {

    void put(String filterKey, String element);

    boolean mightContain(String filterKey, String element);
}