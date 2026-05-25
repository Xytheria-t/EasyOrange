package com.cartethyia.easyorange.framework.idgen;

public interface WorkerIdProvider {

    long getWorkerId();

    void release();
}