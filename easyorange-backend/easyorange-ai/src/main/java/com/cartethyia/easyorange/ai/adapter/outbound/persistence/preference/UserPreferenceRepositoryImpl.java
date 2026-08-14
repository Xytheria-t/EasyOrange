package com.cartethyia.easyorange.ai.adapter.outbound.persistence.preference;

import com.cartethyia.easyorange.ai.chat.UserPreference;
import com.cartethyia.easyorange.ai.chat.UserPreferenceRepository;
import com.cartethyia.easyorange.common.idgen.IdGenerator;
import com.cartethyia.easyorange.common.repository.BaseRepository;
import java.util.List;
import org.springframework.stereotype.Repository;

/**
 * 用户画像仓储（MyBatis-Plus）— upsert 按 (userId, prefKey) 唯一键保证幂等。
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
    public List<UserPreference> findByUserId(String userId) {
        return lambdaQuery().eq(UserPreferenceDO::getUserId, userId).list().stream()
                .map(doc -> new UserPreference(doc.getPrefKey(), doc.getPrefValue()))
                .toList();
    }

    @Override
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
