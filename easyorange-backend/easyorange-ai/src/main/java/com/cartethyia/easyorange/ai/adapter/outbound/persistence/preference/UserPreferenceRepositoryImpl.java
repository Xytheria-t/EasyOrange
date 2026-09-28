package com.cartethyia.easyorange.ai.adapter.outbound.persistence.preference;

import com.cartethyia.easyorange.ai.domain.model.UserPreference;
import com.cartethyia.easyorange.ai.domain.port.UserPreferenceRepository;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import java.util.List;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * 用户画像仓储（MyBatis-Plus）— upsert 按 (userId, prefKey) 唯一键保证幂等。
 * <p>
 * {@code record} 的「查 → 改 或 插」两步必须同事务：缺了它，两个并发的 remember_preference
 * 工具调用会双双查到空、各自插一条，画像里同一个 key 出现重复行，后续读取只能靠取第一条凑合。
 */
@Repository
public class UserPreferenceRepositoryImpl extends BaseRepository<UserPreferenceMapper, UserPreferenceDO>
        implements UserPreferenceRepository {

    private final IdGenerator idGenerator;

    public UserPreferenceRepositoryImpl(UserPreferenceMapper mapper, IdGenerator idGenerator) {
        super(mapper);
        this.idGenerator = idGenerator;
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserPreference> findByUserId(String userId) {
        return lambdaQuery().eq(UserPreferenceDO::getUserId, userId).list().stream()
                .map(doc -> new UserPreference(doc.getPrefKey(), doc.getPrefValue()))
                .toList();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void record(String userId, String key, String value) {
        var existing = lambdaQuery()
                .eq(UserPreferenceDO::getUserId, userId)
                .eq(UserPreferenceDO::getPrefKey, key)
                .one();
        if (existing != null) {
            lambdaUpdate()
                    .eq(UserPreferenceDO::getId, existing.getId())
                    .set(UserPreferenceDO::getPrefValue, value)
                    .update();
            return;
        }
        var entity = new UserPreferenceDO();
        entity.setId(idGenerator.generateId());
        entity.setUserId(userId);
        entity.setPrefKey(key);
        entity.setPrefValue(value);
        mapper.insert(entity);
    }
}
