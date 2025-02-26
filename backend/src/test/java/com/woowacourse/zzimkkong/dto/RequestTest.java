package com.woowacourse.zzimkkong.dto;

import com.woowacourse.zzimkkong.dto.space.EnabledDayOfWeekDto;
import com.woowacourse.zzimkkong.dto.space.SettingRequest;
import org.junit.jupiter.api.BeforeAll;

import javax.validation.ConstraintViolation;
import javax.validation.Validation;
import javax.validation.Validator;
import javax.validation.ValidatorFactory;
import java.util.Set;

import static com.woowacourse.zzimkkong.Constants.*;

class RequestTest {
    private static Validator validator;
    protected final SettingRequest beSettingRequest = new SettingRequest(
            BE_AVAILABLE_START_TIME,
            BE_AVAILABLE_END_TIME,
            BE_RESERVATION_TIME_UNIT.getMinutes(),
            BE_RESERVATION_MINIMUM_TIME_UNIT.getMinutes(),
            BE_RESERVATION_MAXIMUM_TIME_UNIT.getMinutes(),
            EnabledDayOfWeekDto.from(BE_ENABLED_DAY_OF_WEEK),
            0
    );

    @BeforeAll
    static void setUp() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    Set<ConstraintViolation<Object>> getConstraintViolations(Object object) {
        return validator.validate(object);
    }
}
