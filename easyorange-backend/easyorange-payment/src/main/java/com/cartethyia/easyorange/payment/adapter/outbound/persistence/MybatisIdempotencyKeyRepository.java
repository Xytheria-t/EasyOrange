package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.cartethyia.easyorange.common.repository.BaseRepository;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.IdempotencyKeyMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.IdempotencyKeyDO;
import com.cartethyia.easyorange.payment.domain.valueobject.IdempotencyKey;
import com.cartethyia.easyorange.payment.domain.repository.IdempotencyKeyRepositoryPort;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
public class MybatisIdempotencyKeyRepository extends BaseRepository<IdempotencyKeyMapper, IdempotencyKeyDO> implements IdempotencyKeyRepositoryPort {

    public MybatisIdempotencyKeyRepository(IdempotencyKeyMapper mapper) {
        super(mapper);
    }

    @Override
    public void save(IdempotencyKey key) {
        IdempotencyKeyDO po = IdempotencyKeyDO.builder()
                .idempotencyKey(key.key())
                .userId(key.userId())
                .requestHash(key.requestHash())
                .responseData(key.responseData())
                .status(key.status())
                .expiresAt(key.expiresAt())
                .build();
        mapper.insert(po);
    }

    @Override
    public Optional<IdempotencyKey> findByKey(String key) {
        IdempotencyKeyDO po = lambdaQuery()
                .eq(IdempotencyKeyDO::getIdempotencyKey, key)
                .one();
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void updateResponse(String key, String responseData, String status) {
        lambdaUpdate()
                .eq(IdempotencyKeyDO::getIdempotencyKey, key)
                .set(IdempotencyKeyDO::getResponseData, responseData)
                .set(IdempotencyKeyDO::getStatus, status)
                .update();
    }

    @Override
    public void deleteExpiredKeys() {
        lambdaUpdate()
                .lt(IdempotencyKeyDO::getExpiresAt, LocalDateTime.now())
                .remove();
    }

    private IdempotencyKey toDomain(IdempotencyKeyDO po) {
        return new IdempotencyKey(
                po.getIdempotencyKey(),
                po.getUserId(),
                po.getRequestHash(),
                po.getResponseData(),
                po.getStatus(),
                po.getExpiresAt()
        );
    }
}