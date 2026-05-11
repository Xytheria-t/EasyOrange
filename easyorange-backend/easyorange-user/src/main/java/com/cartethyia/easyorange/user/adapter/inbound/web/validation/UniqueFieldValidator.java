package com.cartethyia.easyorange.user.adapter.inbound.web.validation;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class UniqueFieldValidator implements ConstraintValidator<Unique, Object> {

    private static final String ID_COLUMN = "id";
    private static final String DEL_FLAG_COLUMN = "del_flag";

    private final ApplicationContext applicationContext;

    private String fieldName;
    private String idFieldName;
    private Class<?> entityClass;

    @Override
    public void initialize(Unique uniqueAnnotation) {
        this.fieldName = uniqueAnnotation.field();
        this.idFieldName = uniqueAnnotation.idField();
        this.entityClass = uniqueAnnotation.entityClass();
    }

    @Override
    public boolean isValid(Object value, ConstraintValidatorContext context) {
        if (value == null || fieldName == null || fieldName.isBlank()) {
            return true;
        }

        BeanWrapper beanWrapper = new BeanWrapperImpl(value);
        String fieldValue = getPropertyValue(beanWrapper, fieldName);
        if (fieldValue == null || fieldValue.isBlank()) {
            return true;
        }

        @SuppressWarnings("unchecked")
        BaseMapper<Object> mapper = (BaseMapper<Object>) resolveMapper();
        if (mapper == null) {
            log.warn("Unique validation: no Mapper found for entity '{}'", entityClass.getName());
            return true;
        }

        Long idValue = getIdValue(beanWrapper, idFieldName);

        QueryWrapper<Object> wrapper = new QueryWrapper<>();
        wrapper.eq(toUnderlineCase(fieldName), fieldValue);

        if (idValue != null) {
            wrapper.ne(ID_COLUMN, idValue);
        }

        wrapper.eq(DEL_FLAG_COLUMN, 0);
        return mapper.selectCount(wrapper) == 0;
    }

    private String getPropertyValue(BeanWrapper beanWrapper, String property) {
        if (!beanWrapper.isReadableProperty(property)) {
            log.warn("Unique validation: property '{}' not readable on type '{}'",
                property, beanWrapper.getWrappedClass().getName());
            return null;
        }
        Object val = beanWrapper.getPropertyValue(property);
        return val != null ? val.toString() : null;
    }

    private Long getIdValue(BeanWrapper beanWrapper, String property) {
        if (!beanWrapper.isReadableProperty(property)) {
            return null;
        }
        Object val = beanWrapper.getPropertyValue(property);
        if (val == null) {
            return null;
        }
        if (val instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(val.toString());
        } catch (NumberFormatException e) {
            log.warn("Unique validation: id field '{}' is not a valid number", property);
            return null;
        }
    }

    private BaseMapper<?> resolveMapper() {
        String mapperBeanName = deriveMapperBeanName(entityClass.getSimpleName());
        try {
            return applicationContext.getBean(mapperBeanName, BaseMapper.class);
        } catch (Exception e) {
            log.warn("Unique validation: failed to resolve Mapper bean '{}' for entity '{}': {}",
                mapperBeanName, entityClass.getName(), e.getMessage());
            return null;
        }
    }

    private static String deriveMapperBeanName(String entitySimpleName) {
        String baseName = entitySimpleName.endsWith("Entity")
            ? entitySimpleName.substring(0, entitySimpleName.length() - "Entity".length())
            : entitySimpleName;
        char first = baseName.charAt(0);
        String rest = baseName.substring(1);
        return Character.toLowerCase(first) + rest + "Mapper";
    }

    private static String toUnderlineCase(String camelCase) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < camelCase.length(); i++) {
            char c = camelCase.charAt(i);
            if (Character.isUpperCase(c)) {
                if (!sb.isEmpty()) {
                    sb.append('_');
                }
                sb.append(Character.toLowerCase(c));
            } else {
                sb.append(c);
            }
        }
        return sb.toString();
    }
}
