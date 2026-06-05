package com.cartethyia.easyorange.framework.hash;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.Function;

public class ConsistentHashRouter<T extends Node> {

    private static final int DEFAULT_VIRTUAL_NODE_COUNT = 200;

    private final TreeMap<Long, T> ring = new TreeMap<>();
    private final int virtualNodeCount;
    private final Function<String, Long> hashFunction;
    private int actualNodeCount = 0;

    public ConsistentHashRouter(Collection<T> nodes) {
        this(nodes, DEFAULT_VIRTUAL_NODE_COUNT);
    }

    public ConsistentHashRouter(Collection<T> nodes, int virtualNodeCount) {
        this(nodes, virtualNodeCount, ConsistentHashRouter::md5Hash);
    }

    public ConsistentHashRouter(Collection<T> nodes, int virtualNodeCount, Function<String, Long> hashFunction) {
        this.virtualNodeCount = virtualNodeCount;
        this.hashFunction = hashFunction;
        if (nodes != null) {
            for (T node : nodes) {
                addNode(node);
            }
        }
    }

    public void addNode(T node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualKey = node.getKey() + "##VN" + i;
            long hash = hashFunction.apply(virtualKey);
            ring.put(hash, node);
        }
        actualNodeCount++;
    }

    public void removeNode(T node) {
        for (int i = 0; i < virtualNodeCount; i++) {
            String virtualKey = node.getKey() + "##VN" + i;
            long hash = hashFunction.apply(virtualKey);
            ring.remove(hash);
        }
        actualNodeCount--;
    }

    public T route(String key) {
        if (ring.isEmpty()) {
            throw new IllegalStateException("no available node for routing");
        }
        long hash = hashFunction.apply(key);
        Map.Entry<Long, T> entry = ring.ceilingEntry(hash);
        if (entry == null) {
            entry = ring.firstEntry();
        }
        return entry.getValue();
    }

    public Map<Long, T> getRing() {
        return Map.copyOf(ring);
    }

    public int size() {
        return actualNodeCount;
    }

    private static long md5Hash(String key) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] digest = md.digest(key.getBytes(StandardCharsets.UTF_8));
            long hash = 0;
            for (int i = 0; i < 8; i++) {
                hash = (hash << 8) | (digest[i] & 0xFFL);
            }
            return hash & Long.MAX_VALUE;
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("MD5 algorithm not available", e);
        }
    }
}