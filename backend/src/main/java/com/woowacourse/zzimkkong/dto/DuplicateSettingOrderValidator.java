package com.woowacourse.zzimkkong.dto;

import com.woowacourse.zzimkkong.dto.space.SettingRequest;
import org.springframework.util.CollectionUtils;

import javax.validation.ConstraintValidator;
import javax.validation.ConstraintValidatorContext;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public class DuplicateSettingOrderValidator implements ConstraintValidator<NotDuplicatedSettingOrder, List<SettingRequest>> {
    @Override
    public boolean isValid(final List<SettingRequest> value, final ConstraintValidatorContext context) {
        if (CollectionUtils.isEmpty(value)) {
            return true;
        }

        Set<Integer> uniquePriorities = value.stream()
                .map(SettingRequest::getPriorityOrder)
                .collect(Collectors.toSet());

        return value.size() == uniquePriorities.size();
    }
}
