package com.cartethyia.easyorange.order.application.saga.support;

import com.cartethyia.easyorange.order.domain.exception.OrderDomainException;
import com.cartethyia.easyorange.order.domain.saga.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Saga 协调器
 * <p>
 * 负责 Saga 状态的创建、更新、序列化和反序列化
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SagaCoordinator {

    private final SagaRepository sagaRepository;
    private final ObjectMapper objectMapper;

    private static final String SAGA_TYPE = "CREATE_ORDER";

    /**
     * 创建初始 Saga 状态
     *
     * @param command Saga 命令对象
     * @return Saga 状态
     */
    public SagaStatus createInitialStatus(Object command) {
        String sagaId = UUID.randomUUID().toString();
        String payload = serializePayload(sagaId, command);
        return new SagaStatus(
            sagaId,
            SAGA_TYPE,
            SagaState.PENDING,
            "INIT",
            payload,
            null,
            null,
            0,
            LocalDateTime.now(),
            LocalDateTime.now()
        );
    }

    /**
     * 保存 Saga 状态
     */
    public void save(SagaStatus status) {
        sagaRepository.save(status);
    }

    /**
     * 更新 Saga 状态
     */
    public void update(SagaStatus status) {
        sagaRepository.update(status);
    }

    /**
     * 查找 Saga 状态
     */
    public SagaStatus findById(String sagaId) {
        return sagaRepository.findById(sagaId)
            .orElseThrow(() -> new OrderDomainException("Saga 不存在: " + sagaId));
    }

    /**
     * 将 Saga 状态转换到指定状态
     */
    public SagaStatus transitionTo(SagaStatus current, SagaState newState, String step) {
        SagaStatus updated = current.withState(newState).withStep(step);
        sagaRepository.update(updated);
        return updated;
    }

    /**
     * 记录 Saga 错误
     */
    public SagaStatus recordError(SagaStatus current, String errorMessage) {
        SagaStatus updated = current.withError(errorMessage);
        sagaRepository.update(updated);
        return updated;
    }

    /**
     * 记录补偿日志
     */
    public SagaStatus recordCompensationLog(SagaStatus current, String compensationLog) {
        SagaStatus updated = current.withCompensationLog(compensationLog);
        sagaRepository.update(updated);
        return updated;
    }

    /**
     * 反序列化 Saga payload
     *
     * @param sagaId     Saga ID
     * @param payload    payload 字符串
     * @param targetType 目标类型
     * @param <T>        类型参数
     * @return 反序列化后的对象
     * @throws SagaSerializationException 如果反序列化失败
     */
    public <T> T deserializePayload(String sagaId, String payload, Class<T> targetType) {
        try {
            return objectMapper.readValue(payload, targetType);
        } catch (Exception e) {
            throw new SagaSerializationException(sagaId, "反序列化 Saga payload 失败", e);
        }
    }

    /**
     * 序列化 payload
     *
     * @param sagaId Saga ID（用于日志）
     * @param object 要序列化的对象
     * @return 序列化后的字符串
     */
    private String serializePayload(String sagaId, Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("序列化 Saga payload 失败 sagaId={}", sagaId, e);
            // 返回一个简化的 payload，避免完全失败
            return object.toString();
        }
    }

    /**
     * 增加 Saga 重试计数
     */
    public SagaStatus incrementRetry(SagaStatus current) {
        SagaStatus updated = current.withRetry();
        sagaRepository.update(updated);
        return updated;
    }
}