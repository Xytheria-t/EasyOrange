package com.cartethyia.easyorange.framework.idgen;

import com.cartethyia.easyorange.framework.cache.RedisCache;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.concurrent.TimeUnit;

public class RedisWorkerIdProvider implements WorkerIdProvider, InitializingBean, DisposableBean {

    private static final Logger log = LoggerFactory.getLogger(RedisWorkerIdProvider.class);

    private static final String WORKER_ID_KEY = "eo:snowflake:worker_ids";
    private static final long MAX_WORKER_ID = 31;
    private static final long HEARTBEAT_SECONDS = 30;

    private final RedisCache redisCache;
    private final long heartbeatSeconds;
    private long workerId = -1;

    public RedisWorkerIdProvider(RedisCache redisCache) {
        this(redisCache, HEARTBEAT_SECONDS);
    }

    public RedisWorkerIdProvider(RedisCache redisCache, long heartbeatSeconds) {
        this.redisCache = redisCache;
        this.heartbeatSeconds = heartbeatSeconds;
    }

    @Override
    public void afterPropertiesSet() {
        try {
            registerWorkerId();
            startHeartbeat();
        } catch (Exception e) {
            workerId = 0;
            log.warn("action=worker_id_fallback_redis_unavailable workerId=0 error={}", e.getMessage());
        }
    }

    private void registerWorkerId() {
        String hostInfo = getHostInfo();
        for (long id = 0; id <= MAX_WORKER_ID; id++) {
            boolean acquired = redisCache.setIfAbsent(
                    WORKER_ID_KEY + ":" + id,
                    hostInfo,
                    heartbeatSeconds,
                    TimeUnit.SECONDS
            );
            if (acquired) {
                workerId = id;
                log.info("action=worker_id_registered workerId={} host={}", workerId, hostInfo);
                return;
            }
        }
        workerId = 0;
        log.warn("action=worker_id_exhausted fallback=0");
    }

    private void startHeartbeat() {
        Thread heartbeat = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Thread.sleep((heartbeatSeconds / 3) * 1000);
                    if (workerId >= 0) {
                        redisCache.expire(WORKER_ID_KEY + ":" + workerId, heartbeatSeconds, TimeUnit.SECONDS);
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.warn("action=worker_id_heartbeat_failed workerId={}", workerId, e);
                }
            }
        }, "snowflake-heartbeat");
        heartbeat.setDaemon(true);
        heartbeat.start();
    }

    @Override
    public long getWorkerId() {
        if (workerId < 0) {
            registerWorkerId();
        }
        return workerId;
    }

    @Override
    public void release() {
        if (workerId >= 0) {
            redisCache.delete(WORKER_ID_KEY + ":" + workerId);
            log.info("action=worker_id_released workerId={}", workerId);
        }
    }

    @Override
    public void destroy() {
        release();
    }

    private static String getHostInfo() {
        try {
            InetAddress addr = InetAddress.getLocalHost();
            return addr.getHostName() + "@" + addr.getHostAddress();
        } catch (UnknownHostException e) {
            return "unknown@" + Thread.currentThread().getName();
        }
    }
}