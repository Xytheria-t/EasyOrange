package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.support.SFunction;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserEntity;
import com.cartethyia.easyorange.user.adapter.outbound.persistence.UserMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.Map;

@Component
public class UniqueFieldValidator implements ConstraintValidator<Unique, Object> {

    private static final Logger log = LoggerFactory.getLogger(UniqueFieldValidator.class);

    private static final Map<String, SFunction<UserEntity, ?>> FIELD_GETTERS = Map.of(
        "username", UserEntity::getUsername,
        "email", UserEntity::getEmail,
        "phone", UserEntity::getPhone
    );

    private final UserMapper userMapper;

    private String fieldName;
    private String idFieldName;

    public UniqueFieldValidator(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @Override
    public void initialize(Unique uniqueAnnotation) {
        this.fieldName = uniqueAnnotation.field();
        this.idFieldName = uniqueAnnotation.idField();
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || fieldName == null || fieldName.isBlank()) {
            return true;
        }

        String fieldValue = getFieldValue(value, fieldName);
        if (fieldValue == null || fieldValue.isBlank()) {
            return true;
        }

        SFunction<UserEntity, ?> queryMethod = (SFunction<UserEntity, ?>) FIELD_GETTERS.get(fieldName);
        if (queryMethod == null) {
            log.warn("Unique validation: unsupported field '{}'", fieldName);
            return true;
        }

        Long idValue = getIdFieldValue(value, idFieldName);

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(queryMethod, fieldValue);

        if (idValue != null) {
            wrapper.ne(UserEntity::getId, idValue);
        }

        wrapper.eq(UserEntity::getDelFlag, 0);
        return userMapper.selectCount(wrapper) == 0;
    }

    private String getFieldValue(Object obj, String field) {
        try {
            Field declaredField = findField(obj.getClass(), field);
            if (declaredField == null) {
                log.warn("Unique validation: field '{}' not found on class '{}'",
                    field, obj.getClass().getName());
                return null;
            }
            declaredField.setAccessible(true);
            Object val = declaredField.get(obj);
            return val != null ? val.toString() : null;
        } catch (Exception e) {
            log.error("Unique validation: failed to read field '{}' on class '{}'",
                field, obj.getClass().getName(), e);
            return null;
        }
    }

    private Long getIdFieldValue(Object obj, String field) {
        try {
            Field declaredField = findField(obj.getClass(), field);
            if (declaredField == null) {
                return null;
            }
            declaredField.setAccessible(true);
            Object val = declaredField.get(obj);
            return val != null ? ((Number) val).longValue() : null;
        } catch (Exception e) {
            log.error("Unique validation: failed to read id field '{}' on class '{}'",
                field, obj.getClass().getName(), e);
            return null;
        }
    }

    private Field findField(Class<?> clazz, String name) {
        Class<?> current = clazz;
        while (current != null && current != Object.class) {
            try {
                return current.getDeclaredField(name);
            } catch (NoSuchFieldException e) {
                current = current.getSuperclass();
            }
        }
        return null;
    }
}
