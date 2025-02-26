package com.woowacourse.zzimkkong.service;

import com.woowacourse.zzimkkong.domain.*;
import com.woowacourse.zzimkkong.dto.reservation.*;
import com.woowacourse.zzimkkong.exception.map.NoSuchMapException;
import com.woowacourse.zzimkkong.exception.reservation.*;
import com.woowacourse.zzimkkong.exception.setting.MultipleSettingsException;
import com.woowacourse.zzimkkong.exception.setting.NoSettingAvailableException;
import com.woowacourse.zzimkkong.exception.space.NoSuchSpaceException;
import com.woowacourse.zzimkkong.infrastructure.datetime.TimeZoneUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static com.woowacourse.zzimkkong.Constants.*;
import static com.woowacourse.zzimkkong.infrastructure.datetime.TimeZoneUtils.UTC;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class GuestReservationServiceTest extends ServiceTest {
    private static final String CHANGED_NAME = "이름 변경";
    private static final String CHANGED_DESCRIPTION = "회의명 변경";

    @Autowired
    private ReservationService reservationService;

    private ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
            THE_DAY_AFTER_TOMORROW.atTime(11, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
            THE_DAY_AFTER_TOMORROW.atTime(12, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
            RESERVATION_PW,
            RESERVATION_USER_NAME,
            DESCRIPTION);

    private final ReservationPasswordAuthenticationRequest reservationPasswordAuthenticationRequest = new ReservationPasswordAuthenticationRequest(RESERVATION_PW);
    private Map luther;
    private Space be;
    private Space fe;
    private Reservation beAmZeroOne;
    private Reservation bePmOneTwo;
    private Reservation reservation;

    private Long lutherId;
    private Long beId;
    private Long noneExistingMapId;
    private Long noneExistingSpaceId;

    @BeforeEach
    void setUp() {
        Member pobi = Member.builder()
                .email(EMAIL)
                .userName(POBI)
                .emoji(ProfileEmoji.MAN_DARK_SKIN_TONE_TECHNOLOGIST)
                .password(PW)
                .organization(ORGANIZATION)
                .build();
        luther = new Map(1L, LUTHER_NAME, MAP_DRAWING_DATA, MAP_SVG, pobi);

        Setting beSetting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        BE_AVAILABLE_START_TIME,
                        BE_AVAILABLE_END_TIME))
                .reservationTimeUnit(BE_RESERVATION_TIME_UNIT)
                .reservationMinimumTimeUnit(BE_RESERVATION_MINIMUM_TIME_UNIT)
                .reservationMaximumTimeUnit(BE_RESERVATION_MAXIMUM_TIME_UNIT)
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        be = Space.builder()
                .id(1L)
                .name(BE_NAME)
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(BE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(beSetting)))
                .build();

        Setting feSetting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        FE_AVAILABLE_START_TIME,
                        FE_AVAILABLE_END_TIME))
                .reservationTimeUnit(FE_RESERVATION_TIME_UNIT)
                .reservationMinimumTimeUnit(FE_RESERVATION_MINIMUM_TIME_UNIT)
                .reservationMaximumTimeUnit(FE_RESERVATION_MAXIMUM_TIME_UNIT)
                .enabledDayOfWeek(FE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        fe = Space.builder()
                .id(2L)
                .name(FE_NAME)
                .color(FE_COLOR)
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(FE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(feSetting)))
                .build();

        beAmZeroOne = Reservation.builder()
                .id(1L)
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_AM_TEN_ELEVEN_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_AM_TEN_ELEVEN_END_TIME_KST)))
                .description(BE_AM_TEN_ELEVEN_DESCRIPTION)
                .userName(BE_AM_TEN_ELEVEN_USERNAME)
                .password(BE_AM_TEN_ELEVEN_PW)
                .space(be)
                .build();

        bePmOneTwo = Reservation.builder()
                .id(2L)
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_PM_ONE_TWO_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_PM_ONE_TWO_END_TIME_KST)))
                .description(BE_PM_ONE_TWO_DESCRIPTION)
                .userName(BE_PM_ONE_TWO_USERNAME)
                .password(BE_PM_ONE_TWO_PW)
                .space(be)
                .build();

        reservation = makeReservation(
                reservationCreateUpdateWithPasswordRequest.localStartDateTime(),
                reservationCreateUpdateWithPasswordRequest.localEndDateTime(),
                be);

        lutherId = luther.getId();
        beId = be.getId();
        noneExistingMapId = lutherId + 1;
        noneExistingSpaceId = (long) (luther.getSpaces().size() + 1);
    }

    @Test
    @DisplayName("예약 생성 요청 시, mapId와 요청이 들어온다면 예약을 생성한다.")
    void save() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.save(any(Reservation.class)))
                .willReturn(reservation);

        //when
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        ReservationCreateResponse reservationCreateResponse = reservationService.saveReservation(reservationCreateDto);

        //then
        assertThat(reservationCreateResponse.getId()).isEqualTo(reservation.getId());
    }

    @Test
    @DisplayName("예약 생성 요청 시, mapId에 따른 map이 존재하지 않는다면 예외가 발생한다.")
    void saveNotExistMapException() {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.empty());

        // when
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                noneExistingMapId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(NoSuchMapException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, spaceId에 따른 space가 존재하지 않는다면 예외가 발생한다.")
    void saveNotExistSpaceException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                noneExistingSpaceId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(NoSuchSpaceException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 시작 시간이 현재 시간보다 빠르다면 예외가 발생한다.")
    void saveStartTimeBeforeNow() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                LocalDateTime.now().minusHours(3).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                LocalDateTime.now().plusHours(3).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(PastReservationTimeException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 종료 시간이 현재 시간보다 빠르다면 예외가 발생한다.")
    void saveEndTimeBeforeNow() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(14, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(13, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(ImpossibleStartEndTimeException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 시작 시간과 종료 시간이 같다면 예외가 발생한다.")
    void saveStartTimeEqualsEndTime() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        //when
        reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(ImpossibleStartEndTimeException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 시작 시간과 종료 시간의 날짜가 다르다면 예외가 발생한다.")
    void saveStartTimeDateNotEqualsEndTimeDate() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        //when
        reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).plusDays(1).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(NonMatchingStartEndDateException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"60:0", "0:60"}, delimiter = ':')
    @DisplayName("예약 생성 요청 시, 이미 겹치는 시간이 존재하면 예외가 발생한다.")
    void saveAvailabilityException(int startMinute, int endMinute) {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findAllBySpaceIdInAndReservationTimeDate(
                anyList(),
                any(LocalDate.class)))
                .willReturn(List.of(makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusMinutes(startMinute),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().plusMinutes(endMinute),
                        be)));

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(ReservationAlreadyExistsException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 예약이 불가능한 공간이면 에러를 반환한다.")
    void saveReservationUnable() {
        // given, when
        Setting setting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(0, 0),
                        LocalTime.of(18, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(120))
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        Space closedSpace = Space.builder()
                .id(3L)
                .name("백엔드 강의실")
                .color("#FED7D9")
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(false)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(setting)))
                .build();

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        Long closedSpaceId = closedSpace.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                closedSpaceId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(InvalidReservationEnableException.class);
    }

    @Test
    @DisplayName("예약 생성 요청 시, 예약이 불가능한 요일이면 에러를 반환한다.")
    void saveIllegalDayOfWeek() {
        // given, when
        Setting setting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(0, 0),
                        LocalTime.of(18, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(120))
                .enabledDayOfWeek(THE_DAY_AFTER_TOMORROW.plusDays(1L).getDayOfWeek().name())
                .priorityOrder(0)
                .build();

        Space invalidDayOfWeekSpace = Space.builder()
                .id(3L)
                .name("불가능한 요일")
                .color("#FED7D9")
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(true)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(setting)))
                .build();

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        Long invalidDayOfWeekSpaceId = invalidDayOfWeekSpace.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                invalidDayOfWeekSpaceId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(NoSettingAvailableException.class);
    }

    @ParameterizedTest
    @ValueSource(ints = 60)
    @DisplayName("예약 생성 요청 시, 경계값이 일치한다면 생성된다.")
    void saveSameThresholdTime(int duration) {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findAllBySpaceIdInAndDateBetween(
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class)))
                .willReturn(List.of(
                        makeReservation(
                                reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusMinutes(duration),
                                reservationCreateUpdateWithPasswordRequest.localEndDateTime().minusMinutes(duration),
                                be),
                        makeReservation(
                                reservationCreateUpdateWithPasswordRequest.localStartDateTime().plusMinutes(duration),
                                reservationCreateUpdateWithPasswordRequest.localEndDateTime().plusMinutes(duration),
                                be)));

        given(reservations.save(any(Reservation.class)))
                .willReturn(reservation);

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        ReservationCreateResponse reservationCreateResponse = reservationService.saveReservation(reservationCreateDto);
        assertThat(reservationCreateResponse.getId()).isEqualTo(reservation.getId());
    }

    @ParameterizedTest
    @CsvSource({"6,8", "8,10", "22,23"})
    @DisplayName("예약 생성/수정 요청 시, 예약 시간대에 해당하는 예약 조건이 없으면 에러를 반환한다")
    void saveUpdateReservationNoRelevantSetting(int startHour, int endHour) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(startHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(endHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        // be setting: 10:00 ~ 22:00
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(NoSettingAvailableException.class);
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(NoSettingAvailableException.class);
    }

    @ParameterizedTest
    @CsvSource({"7,11", "9,11"})
    @DisplayName("예약 생성/수정 요청 시, 예약 시간대에 해당하는 예약 조건이 2개 이상이면 에러를 반환한다")
    void saveUpdateReservationMultipleSettings(int startHour, int endHour) {
        //given
        be.addSetting(Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(8, 0),
                        LocalTime.of(10, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(60))
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(1)
                .build());
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(startHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(endHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        // be setting: 8:00 ~ 10:00 / 10:00 ~ 22:00
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(MultipleSettingsException.class);
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(MultipleSettingsException.class);
    }

    @ParameterizedTest
    @CsvSource({"9,11", "21,23"})
    @DisplayName("예약 생성/수정 요청 시, 예약 시간대가 예약 조건안에 완전히 포함되지 않고 걸쳐있으면 에러를 반환한다")
    void saveUpdateReservationIsNotWithinSetting(int startHour, int endHour) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(startHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(endHour, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        // be setting: 10:00 ~ 22:00
        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(InvalidStartEndTimeException.class);
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(InvalidStartEndTimeException.class);
    }

    @ParameterizedTest
    @CsvSource({"5,60", "10,55", "5,65", "20,85"})
    @DisplayName("예약 생성/수정 요청 시, space setting의 reservationTimeUnit이 일치하지 않으면 예외가 발생한다.")
    void saveReservationTimeUnitException(int additionalStartMinute, int additionalEndMinute) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        LocalDateTime theDayAfterTomorrowTen = THE_DAY_AFTER_TOMORROW.atTime(10, 0);

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                theDayAfterTomorrowTen.plusMinutes(additionalStartMinute).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                theDayAfterTomorrowTen.plusMinutes(additionalEndMinute).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(InvalidTimeUnitException.class);
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(InvalidTimeUnitException.class);
    }

    @Test
    @DisplayName("예약 생성/수정 요청 시, space setting의 minimum 시간이 옳지 않으면 예외가 발생한다.")
    void saveReservationMinimumDurationTimeException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).plusMinutes(50).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(InvalidMinimumDurationTimeException.class);
    }

    @Test
    @DisplayName("예약 생성/수정 요청 시, space setting의 maximum 시간이 옳지 않으면 예외가 발생한다.")
    void saveReservationMaximumTimeUnitException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).plusMinutes(130).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(InvalidMaximumDurationTimeException.class);
    }

    @Test
    @DisplayName("예약 생성/수정 요청 시, 과거의 예약인 경우 에러가 발생한다.")
    void pastReservationSaveUpdateException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).minusDays(5).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).minusDays(5).plusMinutes(60).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                RESERVATION_USER_NAME,
                DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationCreateDto reservationCreateDto = ReservationCreateDto.of(
                lutherId,
                beId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.saveReservation(reservationCreateDto))
                .isInstanceOf(PastReservationTimeException.class);

        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(PastReservationTimeException.class);
    }

    @Test
    @DisplayName("특정 공간 예약 조회 요청 시, 올바르게 입력하면 해당 날짜, 공간에 대한 예약 정보가 조회된다.")
    void findReservations() {
        //given, when
        int duration = 30;
        List<Reservation> foundReservations = Arrays.asList(
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().minusMinutes(duration),
                        be),
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().plusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().plusMinutes(duration),
                        be));

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findAllBySpaceIdInAndReservationTimeDate(
                anyList(),
                any(LocalDate.class)))
                .willReturn(foundReservations);

        ReservationFindDto reservationFindDto = ReservationFindDto.of(
                lutherId,
                beId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        //then
        ReservationFindResponse reservationFindResponse = ReservationFindResponse.from(foundReservations, Member.builder().build());
        assertThat(reservationService.findReservations(
                reservationFindDto))
                .usingRecursiveComparison()
                .isEqualTo(reservationFindResponse);
    }

    @Test
    @DisplayName("특정 공간 예약 조회 요청 시, 해당하는 맵이 없으면 오류가 발생한다.")
    void findReservationsNotExistMap() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.empty());

        //when
        ReservationFindDto reservationFindDto = ReservationFindDto.of(
                noneExistingMapId,
                beId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.findReservations(
                reservationFindDto))
                .isInstanceOf(NoSuchMapException.class);
    }

    @Test
    @DisplayName("특정 공간 예약 조회 요청 시, 해당하는 공간이 없으면 오류가 발생한다.")
    void findReservationsNotExistSpace() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        //when
        ReservationFindDto reservationFindDto = ReservationFindDto.of(
                lutherId,
                noneExistingSpaceId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.findReservations(
                reservationFindDto))
                .isInstanceOf(NoSuchSpaceException.class);
    }

    @Test
    @DisplayName("전체 예약이나 특정 공간 예약 조회 요청 시, 해당하는 예약이 없으면 빈 정보가 조회된다.")
    void findEmptyReservations() {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(maps.existsById(anyLong()))
                .willReturn(true);
        given(reservations.findAllBySpaceIdInAndDateBetween(
                anyList(),
                any(LocalDate.class),
                any(LocalDate.class)))
                .willReturn(Collections.emptyList());

        //when
        ReservationFindDto reservationFindDto = ReservationFindDto.of(
                lutherId,
                beId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        ReservationFindAllDto reservationFindAllDto = ReservationFindAllDto.of(
                lutherId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        //then
        ReservationFindResponse reservationFindResponse = ReservationFindResponse.from(Collections.emptyList(), Member.builder().build());
        assertThat(reservationService.findReservations(
                reservationFindDto))
                .usingRecursiveComparison()
                .isEqualTo(reservationFindResponse);
        assertThat(reservationService.findAllReservations(
                reservationFindAllDto))
                .usingRecursiveComparison()
                .isEqualTo(ReservationFindAllResponse.of(List.of(be, fe), Collections.emptyList(), Member.builder().build()));
    }

    @Test
    @DisplayName("전체 예약 조회 요청 시, 올바른 mapId, 날짜를 입력하면 해당 날짜에 존재하는 모든 예약 정보가 공간의 Id를 기준으로 정렬되어 조회된다.")
    void findAllReservation() {
        //given, when
        int duration = 30;
        List<Reservation> foundReservations = List.of(
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().minusMinutes(duration),
                        be),
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().plusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().plusMinutes(duration),
                        be),
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().minusMinutes(duration),
                        fe),
                makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().plusMinutes(duration),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().plusMinutes(duration),
                        fe));
        List<Space> findSpaces = List.of(be, fe);

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findAllBySpaceIdInAndReservationTimeDate(
                anyList(),
                any(LocalDate.class)))
                .willReturn(foundReservations);

        ReservationFindAllDto reservationFindAllDto = ReservationFindAllDto.of(
                lutherId,
                THE_DAY_AFTER_TOMORROW,
                ReservationType.Constants.GUEST);

        //then
        ReservationFindAllResponse reservationFindAllResponse = ReservationFindAllResponse.of(
                findSpaces,
                foundReservations,
                Member.builder().build());
        assertThat(reservationService.findAllReservations(reservationFindAllDto))
                .usingRecursiveComparison()
                .isEqualTo(reservationFindAllResponse);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 비밀번호가 일치하는지 확인하고 해당 예약을 반환한다.")
    void findReservation() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservation.getId(),
                new ReservationPasswordAuthenticationRequest(reservation.getPassword()),
                ReservationType.Constants.GUEST);

        ReservationResponse actualResponse = reservationService.findReservation(
                reservationAuthenticationDto);

        //then
        assertThat(actualResponse).usingRecursiveComparison()
                .isEqualTo(ReservationResponse.from(reservation, Member.builder().build()));
    }

    @Test
    @DisplayName("예약 수정 요청 시, 해당 예약이 존재하지 않으면 에러가 발생한다.")
    void findInvalidReservationException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.empty());
        Long reservationId = reservation.getId();
        ReservationPasswordAuthenticationRequest reservationPasswordAuthenticationRequest = new ReservationPasswordAuthenticationRequest("1111");

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.findReservation(
                reservationAuthenticationDto))
                .isInstanceOf(NoSuchReservationException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 비밀번호가 일치하지 않으면 에러가 발생한다.")
    void findWrongPasswordException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        Long reservationId = reservation.getId();
        ReservationPasswordAuthenticationRequest reservationPasswordAuthenticationRequest = new ReservationPasswordAuthenticationRequest("1111");

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.findReservation(
                reservationAuthenticationDto))
                .isInstanceOf(ReservationPasswordException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 올바른 요청이 들어오면 예약이 수정된다.")
    void update() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        Long reservationId = reservation.getId();
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(10, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(11, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                reservation.getPassword(),
                CHANGED_NAME,
                CHANGED_DESCRIPTION);

        //when
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertDoesNotThrow(() -> reservationService.updateReservation(
                reservationUpdateDto));
        assertThat(reservation.getUserName()).isEqualTo(CHANGED_NAME);
        assertThat(reservation.getDescription()).isEqualTo(CHANGED_DESCRIPTION);
    }

    @ParameterizedTest
    @ValueSource(ints = {0, 1})
    @DisplayName("예약 수정 요청 시, 끝 시간 입력이 옳지 않으면 에러가 발생한다.")
    void updateInvalidEndTimeException(int endTime) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn((Optional.of(reservation)));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(12, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(12, 0).minusHours(endTime).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                reservation.getPassword(),
                CHANGED_NAME,
                CHANGED_DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(ImpossibleStartEndTimeException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 시작 시간과 끝 시간이 같은 날짜가 아니면 에러가 발생한다.")
    void updateInvalidDateException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn((Optional.of(reservation)));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                LocalDateTime.now().plusDays(1).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                LocalDateTime.now().plusDays(2).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                reservation.getPassword(),
                CHANGED_NAME,
                CHANGED_DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(NonMatchingStartEndDateException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 비밀번호가 일치하지 않으면 에러가 발생한다.")
    void updateIncorrectPasswordException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                TimeZoneUtils.convertTo(reservation.getStartTime(), ServiceZone.KOREA).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                TimeZoneUtils.convertTo(reservation.getEndTime(), ServiceZone.KOREA).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                "1231",
                CHANGED_NAME,
                CHANGED_DESCRIPTION);
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(ReservationPasswordException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"30:30", "-30:-30"}, delimiter = ':')
    @DisplayName("예약 수정 요청 시, 해당 시간에 예약이 존재하면 에러가 발생한다.")
    void updateImpossibleTimeException(int startTime, int endTime) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        given(reservations.findAllBySpaceIdInAndReservationTimeDate(
                anyList(),
                any(LocalDate.class)))
                .willReturn(Arrays.asList(
                        beAmZeroOne,
                        bePmOneTwo));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                TimeZoneUtils.convertTo(bePmOneTwo.getStartTime().plusMinutes(startTime), ServiceZone.KOREA).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                TimeZoneUtils.convertTo(bePmOneTwo.getEndTime().plusMinutes(endTime), ServiceZone.KOREA).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                reservation.getPassword(),
                reservation.getUserName(),
                reservation.getDescription());
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(ReservationAlreadyExistsException.class);
    }

    @ParameterizedTest
    @CsvSource(value = {"9:10", "21:22"}, delimiter = ':')
    @DisplayName("예약 수정 요청 시, 공간의 예약가능 시간이 아니라면 에러가 발생한다.")
    void updateInvalidTimeSetting(int startTime, int endTime) {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));

        //when
        ReservationCreateUpdateWithPasswordRequest reservationCreateUpdateWithPasswordRequest = new ReservationCreateUpdateWithPasswordRequest(
                THE_DAY_AFTER_TOMORROW.atTime(startTime, 0).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                THE_DAY_AFTER_TOMORROW.atTime(endTime, 30).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone())),
                RESERVATION_PW,
                CHANGED_NAME,
                CHANGED_DESCRIPTION);
        Long reservationId = reservation.getId();
        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                beId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(InvalidStartEndTimeException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 예약이 불가능한 공간이면 에러를 반환한다.")
    void updateReservationUnable() {
        // given, when
        Setting setting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(0, 0),
                        LocalTime.of(18, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(120))
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        Space closedSpace = Space.builder()
                .id(3L)
                .name("예약이 불가능한 공간")
                .color("#FED7D9")
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(false)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(setting)))
                .build();

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        Long closedSpaceId = closedSpace.getId();
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                closedSpaceId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        // then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(InvalidReservationEnableException.class);
    }

    @Test
    @DisplayName("예약 수정 요청 시, 예약이 불가능한 요일이면 에러를 반환한다.")
    void updateIllegalDayOfWeek() {
        // given, when
        Setting setting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        LocalTime.of(0, 0),
                        LocalTime.of(18, 0)))
                .reservationTimeUnit(TimeUnit.from(10))
                .reservationMinimumTimeUnit(TimeUnit.from(10))
                .reservationMaximumTimeUnit(TimeUnit.from(120))
                .enabledDayOfWeek(THE_DAY_AFTER_TOMORROW.plusDays(1L).getDayOfWeek().name())
                .priorityOrder(0)
                .build();

        Space invalidDayOfWeekSpace = Space.builder()
                .id(3L)
                .name("불가능한 요일")
                .color("#FED7D9")
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(true)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(setting)))
                .build();

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(reservation));
        Long invalidDayOfWeekSpaceId = invalidDayOfWeekSpace.getId();
        Long reservationId = reservation.getId();

        ReservationUpdateDto reservationUpdateDto = ReservationUpdateDto.of(
                lutherId,
                invalidDayOfWeekSpaceId,
                reservationId,
                reservationCreateUpdateWithPasswordRequest,
                ReservationType.Constants.GUEST);

        // then
        assertThatThrownBy(() -> reservationService.updateReservation(
                reservationUpdateDto))
                .isInstanceOf(NoSettingAvailableException.class);
    }

    @Test
    @DisplayName("예약 삭제 요청이 옳다면 삭제한다.")
    void deleteReservation() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime(),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime(),
                        be)));
        Long reservationId = reservation.getId();


        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertDoesNotThrow(() -> reservationService.deleteReservation(
                reservationAuthenticationDto));
    }

    @Test
    @DisplayName("예약 삭제 요청 시, 예약이 존재하지 않는다면 오류가 발생한다.")
    void deleteReservationException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.empty());
        Long reservationId = reservation.getId();

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.deleteReservation(
                reservationAuthenticationDto))
                .isInstanceOf(NoSuchReservationException.class);
    }

    @Test
    @DisplayName("예약 삭제 요청 시, 비밀번호가 일치하지 않는다면 오류가 발생한다.")
    void deleteReservationPasswordException() {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime(),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime(),
                        be)));

        ReservationPasswordAuthenticationRequest reservationPasswordAuthenticationRequest
                = new ReservationPasswordAuthenticationRequest("1233");
        Long reservationId = reservation.getId();

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.deleteReservation(
                reservationAuthenticationDto))
                .isInstanceOf(ReservationPasswordException.class);
    }

    @Test
    @DisplayName("예약 삭제 요청 시, 과거의 예약이면 오류가 발생한다.")
    void deletePastReservationException() {
        //given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findById(anyLong()))
                .willReturn(Optional.of(makeReservation(
                        reservationCreateUpdateWithPasswordRequest.localStartDateTime().minusDays(5),
                        reservationCreateUpdateWithPasswordRequest.localEndDateTime().minusDays(5),
                        be)));

        ReservationPasswordAuthenticationRequest reservationPasswordAuthenticationRequest
                = new ReservationPasswordAuthenticationRequest(reservationCreateUpdateWithPasswordRequest.getPassword());
        Long reservationId = reservation.getId();

        //when
        ReservationAuthenticationDto reservationAuthenticationDto = ReservationAuthenticationDto.of(
                lutherId,
                beId,
                reservationId,
                reservationPasswordAuthenticationRequest,
                ReservationType.Constants.GUEST);

        //then
        assertThatThrownBy(() -> reservationService.deleteReservation(
                reservationAuthenticationDto))
                .isInstanceOf(DeleteExpiredReservationException.class);
    }

    private Reservation makeReservation(final LocalDateTime startTime, final LocalDateTime endTime, final Space space) {
        return Reservation.builder()
                .id(3L)
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                startTime,
                                endTime))
                .password(reservationCreateUpdateWithPasswordRequest.getPassword())
                .userName(reservationCreateUpdateWithPasswordRequest.getName())
                .description(reservationCreateUpdateWithPasswordRequest.getDescription())
                .space(space)
                .build();
    }
}
