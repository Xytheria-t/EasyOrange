package com.cartethyia.easyorange.user.infrastructure.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.cartethyia.easyorange.user.common.constant.UserConstant;
import com.cartethyia.easyorange.user.domain.model.User;
import com.cartethyia.easyorange.user.domain.repository.UserRepository;
import com.cartethyia.easyorange.user.infrastructure.persistence.UserEntity;
import com.cartethyia.easyorange.user.infrastructure.persistence.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
@RequiredArgsConstructor
public class UserRepositoryImpl implements UserRepository {

    private final UserMapper userMapper;

    @Override
    public Optional<User> findById(Long id) {
        return Optional.ofNullable(userMapper.selectById(id))
            .map(this::toDomain);
    }

    @Override
    public List<User> findAllById(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return userMapper.selectBatchIds(ids).stream()
            .map(this::toDomain)
            .toList();
    }

    @Override
    public Optional<User> findByUsername(String username) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getUsername, username);
        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(this::toDomain);
    }

    @Override
    public Optional<User> findByPhone(String phone) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getPhone, phone);
        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(this::toDomain);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(UserEntity::getEmail, email);
        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(this::toDomain);
    }

    @Override
    public Optional<User> findByAccount(String account) {
        if (account == null || account.isBlank()) {
            return Optional.empty();
        }

        boolean isEmail = UserConstant.EMAIL_PATTERN.matcher(account).matches();
        boolean isPhone = UserConstant.PHONE_PATTERN.matcher(account).matches();

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();

        if (isEmail) {
            wrapper.eq(UserEntity::getEmail, account)
                .or()
                .eq(UserEntity::getPhone, account);
        } else if (isPhone) {
            wrapper.eq(UserEntity::getPhone, account);
        }

        wrapper.or().eq(UserEntity::getUsername, account);

        return Optional.ofNullable(userMapper.selectOne(wrapper))
            .map(this::toDomain);
    }

    @Override
    public User save(User user) {
        UserEntity entity = toEntity(user);
        userMapper.insert(entity);
        user.setId(entity.getId());
        return user;
    }

    @Override
    public boolean update(User user) {
        UserEntity entity = toEntity(user);
        return userMapper.updateById(entity) > 0;
    }

    @Override
    public boolean updatePassword(Long userId, String encodedPassword) {
        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserEntity::getId, userId)
            .set(UserEntity::getPassword, encodedPassword)
            .set(UserEntity::getPwdUpdateDate, LocalDateTime.now());
        return userMapper.update(null, wrapper) > 0;
    }

    @Override
    public boolean updateLoginInfo(Long userId, String loginIp) {
        LambdaUpdateWrapper<UserEntity> wrapper = new LambdaUpdateWrapper<>();
        wrapper.eq(UserEntity::getId, userId)
            .set(UserEntity::getLoginDate, LocalDateTime.now())
            .set(UserEntity::getLoginIp, loginIp);
        return userMapper.update(null, wrapper) > 0;
    }

    @Override
    public void deleteById(Long id) {
        userMapper.deleteById(id);
    }

    private User toDomain(UserEntity entity) {
        return User.builder()
            .id(entity.getId())
            .username(entity.getUsername())
            .password(entity.getPassword())
            .userType(entity.getUserType())
            .email(entity.getEmail())
            .phone(entity.getPhone())
            .studentId(entity.getStudentId())
            .realName(entity.getRealName())
            .nickName(entity.getNickName())
            .sex(entity.getSex())
            .status(entity.getStatus())
            .loginIp(entity.getLoginIp())
            .loginDate(entity.getLoginDate())
            .pwdUpdateDate(entity.getPwdUpdateDate())
            .avatar(entity.getAvatar())
            .remark(entity.getRemark())
            .createTime(entity.getCreateTime())
            .updateTime(entity.getUpdateTime())
            .createBy(entity.getCreateBy())
            .updateBy(entity.getUpdateBy())
            .delFlag(entity.getDelFlag())
            .version(entity.getVersion())
            .build();
    }

    private UserEntity toEntity(User domain) {
        UserEntity entity = UserEntity.builder()
            .id(domain.getId())
            .username(domain.getUsername())
            .password(domain.getPassword())
            .userType(domain.getUserType())
            .email(domain.getEmail())
            .phone(domain.getPhone())
            .studentId(domain.getStudentId())
            .realName(domain.getRealName())
            .nickName(domain.getNickName())
            .sex(domain.getSex())
            .status(domain.getStatus())
            .loginIp(domain.getLoginIp())
            .loginDate(domain.getLoginDate())
            .pwdUpdateDate(domain.getPwdUpdateDate())
            .avatar(domain.getAvatar())
            .remark(domain.getRemark())
            .build();
        if (domain.getCreateTime() != null) {
            entity.setCreateTime(domain.getCreateTime());
        }
        if (domain.getUpdateTime() != null) {
            entity.setUpdateTime(domain.getUpdateTime());
        }
        return entity;
    }
}
