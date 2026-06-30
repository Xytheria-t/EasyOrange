package com.cartethyia.easyorange.framework.idgen;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

public class SnowflakeIdGenerator implements IdGenerator {

    private static final Logger log = LoggerFactory.getLogger(SnowflakeIdGenerator.class);

    private static final long EPOCH = LocalDate.of(2024, 1, 1)
            .atStartOfDay(ZoneId.systemDefault())
            .toInstant()
            .toEpochMilli();

    private static final long WORKER_ID_BITS = 5L;
    private static final long DATA_CENTER_ID_BITS = 5L;
    private static final long SEQUENCE_BITS = 12L;

    private static final long MAX_WORKER_ID = ~(-1L << WORKER_ID_BITS);
    private static final long MAX_DATA_CENTER_ID = ~(-1L << DATA_CENTER_ID_BITS);

    private static final long WORKER_ID_SHIFT = SEQUENCE_BITS;
    private static final long DATA_CENTER_ID_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS;
    private static final long TIMESTAMP_SHIFT = SEQUENCE_BITS + WORKER_ID_BITS + DATA_CENTER_ID_BITS;
    private static final long SEQUENCE_MASK = ~(-1L << SEQUENCE_BITS);

    private static final long CLOCK_BACKWARD_TOLERANCE_MS = 10;

    private final long workerId;
    private final long dataCenterId;
    private long sequence = 0L;
    private long lastTimestamp = -1L;
    private long clockBackwardCount = 0;

    public SnowflakeIdGenerator(WorkerIdProvider workerIdProvider, long dataCenterId) {
        if (dataCenterId > MAX_DATA_CENTER_ID || dataCenterId < 0) {
            throw new IllegalArgumentException("Data center ID must be between 0 and " + MAX_DATA_CENTER_ID);
        }
        this.workerId = workerIdProvider.getWorkerId();
        this.dataCenterId = dataCenterId;
        log.info("action=snowflake_init workerId={} dataCenterId={}", workerId, dataCenterId);
    }

    public synchronized long nextId() {
        long timestamp = timeGen();

        if (timestamp < lastTimestamp) {
            long diff = lastTimestamp - timestamp;
            if (diff <= CLOCK_BACKWARD_TOLERANCE_MS) {
                clockBackwardCount++;
                log.warn("action=clock_backward_tolerated diff={}ms count={}", diff, clockBackwardCount);
                timestamp = lastTimestamp;
            } else {
                log.error("action=clock_backward_exceeded diff={}ms", diff);
                throw new IllegalStateException(
                        "Clock moved backwards " + diff + " ms (tolerance: " + CLOCK_BACKWARD_TOLERANCE_MS + " ms)");
            }
        }

        if (lastTimestamp == timestamp) {
            sequence = (sequence + 1) & SEQUENCE_MASK;
            if (sequence == 0) {
                timestamp = tilNextMillis(lastTimestamp);
            }
        } else {
            sequence = 0L;
            clockBackwardCount = 0;
        }

        lastTimestamp = timestamp;

        return ((timestamp - EPOCH) << TIMESTAMP_SHIFT)
                | (dataCenterId << DATA_CENTER_ID_SHIFT)
                | (workerId << WORKER_ID_SHIFT)
                | sequence;
    }

    private long tilNextMillis(long lastTimestamp) {
        long timestamp = timeGen();
        while (timestamp <= lastTimestamp) {
            timestamp = timeGen();
        }
        return timestamp;
    }

    private long timeGen() {
        return Instant.now().toEpochMilli();
    }

    @Override
    public String generateId() {
        return String.valueOf(nextId());
    }

    public long getWorkerId() {
        return workerId;
    }

    public long getDataCenterId() {
        return dataCenterId;
    }

    public long getClockBackwardCount() {
        return clockBackwardCount;
    }
}