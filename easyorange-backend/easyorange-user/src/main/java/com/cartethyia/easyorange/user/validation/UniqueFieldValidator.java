package com.cartethyia.easyorange.user.validation;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.cartethyia.easyorange.user.entity.User;
import com.cartethyia.easyorange.user.mapper.UserMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;

@Component
public class UniqueFieldValidator implements ConstraintValidator<Unique, Object> {

    @Autowired
    private UserMapper userMapper;

    private String fieldName;

    @Override
    public void initialize(Unique uniqueAnnotation) {
        this.fieldName = uniqueAnnotation.field();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || fieldName == null || fieldName.isEmpty()) {
            return true;
        }

        String fieldValue = getFieldValue(value);
        if (fieldValue == null || fieldValue.isEmpty()) {
            return true;
        }

        Long userId = getUserId(value);

        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        switch (fieldName) {
            case "username":
                wrapper.eq(User::getUsername, fieldValue);
                break;
            case "email":
                wrapper.eq(User::getEmail, fieldValue);
                break;
            case "phone":
                wrapper.eq(User::getPhone, fieldValue);
                break;
            default:
                return true;
        }

        if (userId != null) {
            wrapper.ne(User::getId, userId);
        }

        wrapper.eq(User::getDelFlag, 0);
        return userMapper.selectCount(wrapper) == 0;
    }

    private String getFieldValue(Object obj) {
        try {
            Field field = obj.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            return (String) field.get(obj);
        } catch (Exception e) {
            return null;
        }
    }

    private Long getUserId(Object obj) {
        try {
            Field idField = obj.getClass().getDeclaredField("id");
            idField.setAccessible(true);
            Object idValue = idField.get(obj);
            return idValue != null ? (Long) idValue : null;
        } catch (Exception e) {
            return null;
        }
    }
}