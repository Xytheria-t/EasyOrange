package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.cartethyia.easyorange.framework.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.IdempotencyKeyMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.IdempotencyKeyPO;
import com.cartethyia.easyorange.payment.domain.valueobject.IdempotencyKey;
import com.cartethyia.easyorange.payment.domain.port.output.IdempotencyKeyRepositoryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MybatisIdempotencyKeyRepository extends BaseRepository<IdempotencyKeyMapper, IdempotencyKeyPO> implements IdempotencyKeyRepositoryPort {

    public MybatisIdempotencyKeyRepository(IdempotencyKeyMapper mapper) {
        super(mapper);
    }

    @Override
    public void save(IdempotencyKey key) {
        IdempotencyKeyPO po = IdempotencyKeyPO.builder()
                .idempotencyKey(key.getKey())
                .userId(key.getUserId())
                .requestHash(key.getRequestHash())
                .responseData(key.getResponseData())
                .status(key.getStatus())
                .expiresAt(key.getExpiresAt())
                .build();
        mapper.insert(po);
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        IdempotencyKeyPO po = lambdaQuery()
                .eq(IdempotencyKeyPO::getIdempotencyKey, key)
                .one();
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void updateResponse(String key, String responseData, String status) {
        lambdaUpdate()
                .eq(IdempotencyKeyPO::getIdempotencyKey, key)
                .set(IdempotencyKeyPO::getResponseData, responseData)
                .set(IdempotencyKeyPO::getStatus, status)
                .update();
    }

    @Override
    public void deleteExpiredKeys() {
        lambdaUpdate()
                .lt(IdempotencyKeyPO::getExpiresAt, LocalDateTime.now())
                .remove();
    }

    private IdempotencyKey toDomain(IdempotencyKeyPO po) {
        return IdempotencyKey.builder()
                .key(po.getIdempotencyKey())
                .userId(po.getUserId())
                .requestHash(po.getRequestHash())
                .responseData(po.getResponseData())
                .status(po.getStatus())
                .expiresAt(po.getExpiresAt())
                .build();
    }
}