package com.cartethyia.easyorange.framework.redis;

import org.springframework.data.redis.core.script.DefaultRedisScript;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public interface RedisCache {

    <T> void set(String key, T value);

    <T> void set(String key, T value, long timeout, TimeUnit timeUnit);

    <T> T get(String key);

    <T> T get(String key, Class<T> type);

    // ==================== Bitmap 操作 ====================

    Boolean setBit(String key, long offset, boolean value);

    Boolean getBit(String key, long offset);

    Long bitCount(String key);

    // ==================== Key 生命周期 ====================

    Boolean expire(String key, long timeout, TimeUnit timeUnit);

    long getExpire(String key, TimeUnit timeUnit);

    Boolean hasKey(String key);

    boolean delete(String key);

    Long delete(Collection<String> keys);

    <T> Boolean setIfAbsent(String key, T value);

    <T> Boolean setIfAbsent(String key, T value, long timeout, TimeUnit timeUnit);

    Long increment(String key);

    Long increment(String key, long delta);

    Long decrement(String key);

    Long decrement(String key, long delta);

    <T> Map<String, T> multiGet(Collection<String> keys);

    <T> Map<String, T> multiGet(Collection<String> keys, Class<T> type);

    <T> void multiSet(Map<String, T> map);

    Boolean tryLock(String key, String value, long timeout, TimeUnit timeUnit);

    Boolean unlock(String key, Object value);

    Boolean unlockIfValueMatches(String key, String expectedValue);

    <T> void hashPut(String key, String hashKey, T value);

    <T> void hashPutAll(String key, Map<String, T> map);

    Boolean hashPutIfAbsent(String key, String hashKey, Object value);

    <T> T hashGet(String key, String hashKey);

    <T> Map<String, T> hashGetAll(String key);

    Boolean hashHasKey(String key, String hashKey);

    Long hashDelete(String key, Object... hashKeys);

    Long hashSize(String key);

    Long hashIncrement(String key, String hashKey, long delta);

    <T> Long listPush(String key, T value);

    <T> Long listPushLeft(String key, T value);

    <T> T listPop(String key, Class<T> type);

    <T> T listPopRight(String key, Class<T> type);

    <T> List<T> listRange(String key, long start, long end);

    Long listSize(String key);

    <T> Boolean setAdd(String key, T... values);

    <T> Set<T> setMembers(String key, Class<T> type);

    Boolean setIsMember(String key, Object value);

    Long setRemove(String key, Object... values);

    Long setSize(String key);

    <T> Boolean zsetAdd(String key, double score, T value);

    <T> Set<T> zsetRangeByScore(String key, double min, double max, Class<T> type);

    <T> Set<T> zsetRangeByScoreWithScores(String key, double min, double max);

    Double zsetScore(String key, Object value);

    Long zsetRemove(String key, Object... values);

    Long zsetSize(String key);

    Long executeLuaScript(DefaultRedisScript<Long> script, List<String> keys, Object... args);

    Set<String> keys(String pattern);
}