package com.cartethyia.easyorange.order.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaRepository;
import com.cartethyia.easyorange.order.domain.saga.SagaState;
import com.cartethyia.easyorange.order.domain.saga.SagaStatus;
import tools.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Slf4j
@Repository
public class SagaRepositoryImpl extends BaseRepository<SagaMapper, SagaDO> implements SagaRepository {

    private final ObjectMapper objectMapper;

    public SagaRepositoryImpl(SagaMapper sagaMapper, ObjectMapper objectMapper) {
        super(sagaMapper);
        this.objectMapper = objectMapper;
    }

    @Override
    public void save(SagaStatus sagaStatus) {
        SagaDO sagaDO = toDataObject(sagaStatus);
        mapper.insert(sagaDO);
    }

    @Override
    public Optional<SagaStatus> findById(String sagaId) {
        SagaDO sagaDO = mapper.selectById(sagaId);
        return Optional.ofNullable(sagaDO).map(this::toDomain);
    }

    @Override
    public Optional<SagaStatus> findByOrderId(Long orderId) {
        SagaDO sagaDO = lambdaQuery()
                .eq(SagaDO::getPayload, orderId.toString())
                .orderByDesc(SagaDO::getCreatedAt)
                .last("LIMIT 1")
                .one();
        return Optional.ofNullable(sagaDO).map(this::toDomain);
    }

    @Override
    public void update(SagaStatus sagaStatus) {
        updateById(toDataObject(sagaStatus));
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
