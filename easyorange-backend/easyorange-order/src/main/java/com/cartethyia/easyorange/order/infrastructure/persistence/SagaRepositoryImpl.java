package com.cartethyia.easyorange.order.infrastructure.persistence;

import com.cartethyia.easyorange.order.domain.saga.SagaRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Repository
@RequiredArgsConstructor
public class SagaRepositoryImpl implements SagaRepository {

    private final SagaMapper sagaMapper;
    private final ObjectMapper objectMapper;

    @Override
    public void save(SagaStatus sagaStatus) {
        SagaDO sagaDO = toDataObject(sagaStatus);
        sagaMapper.insert(sagaDO);
    }

    @Override
    public Optional<SagaStatus> findById(String sagaId) {
        SagaDO sagaDO = sagaMapper.selectById(sagaId);
        return Optional.ofNullable(sagaDO).map(this::toDomain);
    }

    @Override
    public Optional<SagaStatus> findByOrderId(Long orderId) {
        SagaDO sagaDO = sagaMapper.selectOne(
            new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<SagaDO>()
                .eq(SagaDO::getPayload, orderId.toString())
                .orderByDesc(SagaDO::getCreatedAt)
                .last("LIMIT 1")
        );
        return Optional.ofNullable(sagaDO).map(this::toDomain);
    }

    @Override
    public void update(SagaStatus sagaStatus) {
        SagaDO sagaDO = toDataObject(sagaStatus);
        sagaMapper.updateById(sagaDO);
    }

    private SagaDO toDataObject(SagaStatus sagaStatus) {
        SagaDO sagaDO = new SagaDO();
        sagaDO.setSagaId(sagaStatus.sagaId());
        sagaDO.setSagaType(sagaStatus.sagaType());
        sagaDO.setState(sagaStatus.state().name());
        sagaDO.setCurrentStep(sagaStatus.currentStep());
        sagaDO.setPayload(sagaStatus.payload());
        sagaDO.setErrorMessage(sagaStatus.errorMessage());
        sagaDO.setCompensationLog(sagaStatus.compensationLog());
        sagaDO.setRetryCount(sagaStatus.retryCount());
        sagaDO.setCreatedAt(sagaStatus.createdAt());
        sagaDO.setUpdatedAt(sagaStatus.updatedAt());
        return sagaDO;
    }

    private SagaStatus toDomain(SagaDO sagaDO) {
        return new SagaStatus(
            sagaDO.getSagaId(),
            sagaDO.getSagaType(),
            SagaState.valueOf(sagaDO.getState()),
            sagaDO.getCurrentStep(),
            sagaDO.getPayload(),
            sagaDO.getErrorMessage(),
            sagaDO.getCompensationLog(),
            sagaDO.getRetryCount(),
            sagaDO.getCreatedAt(),
            sagaDO.getUpdatedAt()
        );
    }
}
