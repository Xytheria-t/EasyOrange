package com.cartethyia.easyorange.payment.adapter.outbound.persistence;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.mapper.IdempotencyKeyMapper;
import com.cartethyia.easyorange.payment.adapter.outbound.persistence.po.IdempotencyKeyPO;
import com.cartethyia.easyorange.payment.domain.idempotency.IdempotencyKey;
import com.cartethyia.easyorange.payment.domain.idempotency.IdempotencyKeyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class MybatisIdempotencyKeyRepository implements IdempotencyKeyRepository {

    private final IdempotencyKeyMapper mapper;

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
        LambdaQueryWrapper<IdempotencyKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(IdempotencyKeyPO::getIdempotencyKey, key);
        IdempotencyKeyPO po = mapper.selectOne(wrapper);
        return Optional.ofNullable(po).map(this::toDomain);
    }

    @Override
    public void updateResponse(String key, String responseData, String status) {
        LambdaUpdateWrapper<IdempotencyKeyPO> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(IdempotencyKeyPO::getIdempotencyKey, key)
                .set(IdempotencyKeyPO::getResponseData, responseData)
                .set(IdempotencyKeyPO::getStatus, status);
        mapper.update(null, wrapper);
    }

    @Override
    public void deleteExpiredKeys() {
        LambdaQueryWrapper<IdempotencyKeyPO> wrapper = new LambdaQueryWrapper<>();
        wrapper.lt(IdempotencyKeyPO::getExpiresAt, LocalDateTime.now());
        mapper.delete(wrapper);
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
