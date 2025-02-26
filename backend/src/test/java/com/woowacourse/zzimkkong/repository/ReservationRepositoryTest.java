package com.woowacourse.zzimkkong.repository;

import com.woowacourse.zzimkkong.domain.*;
import com.woowacourse.zzimkkong.infrastructure.datetime.TimeZoneUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.Sort;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static com.woowacourse.zzimkkong.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class ReservationRepositoryTest extends RepositoryTest {
    private Space be;
    private Space fe;

    private Reservation beAmZeroOne;
    private Reservation bePmOneTwo;
    private Reservation beNextDayAmSixTwelve;
    private Reservation fe1ZeroOne;
    private Reservation bePmTwoThreeByPobi;
    private Reservation beFiveDaysAgoPmTwoThreeByPobi;

    private Member pobi;

    @BeforeEach
    void setUp() {
        pobi = Member.builder()
                .email(EMAIL)
                .userName(POBI)
                .emoji(ProfileEmoji.MAN_DARK_SKIN_TONE_TECHNOLOGIST)
                .password(PW)
                .organization(ORGANIZATION)
                .build();
        Map luther = new Map(LUTHER_NAME, MAP_DRAWING_DATA, MAP_SVG, pobi);

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
                .name(BE_NAME)
                .color(BE_COLOR)
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
                .name(FE_NAME)
                .color(FE_COLOR)
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(FE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(feSetting)))
                .build();

        members.save(pobi);
        maps.save(luther);
        spaces.save(be);
        spaces.save(fe);

        beAmZeroOne = Reservation.builder()
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
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_PM_ONE_TWO_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_PM_ONE_TWO_END_TIME_KST)))
                .description(BE_PM_ONE_TWO_DESCRIPTION)
                .userName(BE_PM_ONE_TWO_USERNAME)
                .password(BE_PM_ONE_TWO_PW)
                .space(be)
                .build();

        beNextDayAmSixTwelve = Reservation.builder()
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_NEXT_DAY_PM_FOUR_TO_SIX_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_NEXT_DAY_PM_FOUR_TO_SIX_END_TIME_KST)))
                .description(BE_NEXT_DAY_PM_FOUR_TO_SIX_DESCRIPTION)
                .userName(BE_NEXT_DAY_PM_FOUR_TO_SIX_USERNAME)
                .password(BE_NEXT_DAY_PM_FOUR_TO_SIX_PW)
                .space(be)
                .build();

        fe1ZeroOne = Reservation.builder()
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(FE1_AM_TEN_ELEVEN_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(FE1_AM_TEN_ELEVEN_END_TIME_KST)))
                .description(FE1_AM_TEN_ELEVEN_DESCRIPTION)
                .userName(FE1_AM_TEN_ELEVEN_USERNAME)
                .password(FE1_AM_TEN_ELEVEN_PW)
                .space(fe)
                .build();

        bePmTwoThreeByPobi = Reservation.builder()
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_PM_TWO_THREE_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_PM_TWO_THREE_END_TIME_KST)))
                .description(BE_PM_TWO_THREE_DESCRIPTION)
                .userName(pobi.getUserName())
                .member(pobi)
                .space(be)
                .build();

        beFiveDaysAgoPmTwoThreeByPobi = Reservation.builder()
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                TimeZoneUtils.convertToUTC(BE_FIVE_DAYS_AGO_PM_TWO_THREE_START_TIME_KST),
                                TimeZoneUtils.convertToUTC(BE_FIVE_DAYS_AGO_PM_TWO_THREE_END_TIME_KST)))
                .description(BE_FIVE_DAYS_AGO_PM_TWO_THREE_DESCRIPTION)
                .userName(pobi.getUserName())
                .member(pobi)
                .space(be)
                .build();

        reservations.save(beAmZeroOne);
        reservations.save(bePmOneTwo);
        reservations.save(beNextDayAmSixTwelve);
        reservations.save(fe1ZeroOne);
        reservations.save(bePmTwoThreeByPobi);
        reservations.save(beFiveDaysAgoPmTwoThreeByPobi);
    }

    @Test
    @DisplayName("예약을 추가할 수 있다.")
    void save() {
        //given
        Reservation be_two_three = Reservation.builder()
                .reservationTime(
                        ReservationTime.ofDefaultServiceZone(
                                THE_DAY_AFTER_TOMORROW.atTime(2, 0),
                                THE_DAY_AFTER_TOMORROW.atTime(3, 0)))
                .description("찜꽁 4차 회의")
                .userName("찜꽁")
                .password("1234")
                .space(be)
                .build();

        //when
        final Reservation savedReservation = reservations.save(be_two_three);

        //then
        assertThat(savedReservation.getId()).isNotNull();
        assertThat(savedReservation).isEqualTo(be_two_three);
    }

    @Test
    @DisplayName("map id, space id, 특정 날짜가 주어질 때, 해당 spaceId와 해당 날짜 전,훗날에 속하는 예약들을 찾아온다")
    void findAllBySpaceIdInAndDateGreaterThanEqualAndDateLessThanEqual() {
        // given, when
        List<Reservation> foundReservations = getReservations(
                List.of(be.getId(), fe.getId()),
                THE_DAY_AFTER_TOMORROW);

        // then
        assertThat(foundReservations).usingRecursiveComparison()
                .ignoringCollectionOrder()
                .isEqualTo(List.of(beAmZeroOne, beNextDayAmSixTwelve, bePmOneTwo, fe1ZeroOne, bePmTwoThreeByPobi));
    }

    @Test
    @DisplayName("특정 날짜에 부합하는 예약이 없으면 빈 리스트를 반환한다")
    void findAllBySpaceIdInAndDateGreaterThanEqualAndDateLessThanEqual_noMatchingTimeAndSpace() {
        // given, when
        List<Reservation> foundReservationsBe = getReservations(
                List.of(be.getId()),
                THE_DAY_AFTER_TOMORROW.plusDays(3));
        List<Reservation> foundReservationsFe = getReservations(
                List.of(fe.getId()),
                THE_DAY_AFTER_TOMORROW.minusDays(3));

        // then
        assertThat(foundReservationsBe).isEmpty();
        assertThat(foundReservationsFe).isEmpty();
    }

    @Test
    @DisplayName("예약을 삭제할 수 있다.")
    void delete() {
        //given, when, then
        assertDoesNotThrow(() -> reservations.delete(beNextDayAmSixTwelve));
    }

    @Test
    @DisplayName("해당 공간에 대한 예약 존재여부를 확인한다.")
    void existsBySpace() {
        // given, when, then
        assertThat(reservations.existsBySpaceIdAndReservationTimeEndTimeAfter(fe.getId(), LocalDateTime.now())).isTrue();

        reservations.delete(fe1ZeroOne);
        assertThat(reservations.existsBySpaceIdAndReservationTimeEndTimeAfter(fe.getId(), LocalDateTime.now())).isFalse();
    }

    @ParameterizedTest
    @CsvSource(value = {"0:false", "30:true"}, delimiter = ':')
    @DisplayName("특정 시간 이후의 예약이 존재하는지 확인한다.")
    void existsBySpaceIdAndDateGreaterThanEqual(int minusMinute, boolean expected) {
        //given, when
        Boolean actual = reservations.existsBySpaceIdAndReservationTimeEndTimeAfter(
                be.getId(),
                TimeZoneUtils.convertToUTC(BE_NEXT_DAY_PM_FOUR_TO_SIX_END_TIME_KST).minusMinutes(minusMinute));

        //then
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    @DisplayName("page로 모든 예약을 조회한다.")
    void findAllByPaging() {
        // given, when
        PageRequest pageRequest = PageRequest.of(0, 20, Sort.unsorted());
        Page<Reservation> actual = reservations.findAllByFetch(pageRequest);

        // then
        assertThat(actual.getSize()).isEqualTo(20);
        assertThat(actual.getContent()).hasSize(6);
        assertThat(actual.getContent()).usingRecursiveComparison()
                .isEqualTo(List.of(beAmZeroOne, bePmOneTwo, beNextDayAmSixTwelve, fe1ZeroOne, bePmTwoThreeByPobi, beFiveDaysAgoPmTwoThreeByPobi));
    }

    private List<Reservation> getReservations(List<Long> spaceIds, LocalDate date) {
        return reservations.findAllBySpaceIdInAndDateBetween(
                spaceIds,
                date.minusDays(1L),
                date.plusDays(1L));
    }

    @Test
    @DisplayName("로그인한 예약자의 특정 시간 이후 (Inclusive) 예약 내역을 조회한다")
    void findAllByMemberAndReservationTimeDateGreaterThanEqualAndReservationTimeStartTimeGreaterThanEqual() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("reservationTime.startTime"));
        Slice<Reservation> actual = reservations.findAllByMemberAndReservationTimeDateGreaterThanEqualAndReservationTimeEndTimeGreaterThanEqual(
                pobi,
                THE_DAY_AFTER_TOMORROW,
                TimeZoneUtils.convertToUTC(BE_PM_TWO_THREE_START_TIME_KST),
                pageRequest);

        List<Reservation> expectedContent = List.of(bePmTwoThreeByPobi);
        assertThat(actual.getContent()).isEqualTo(expectedContent);
    }

    @Test
    @DisplayName("로그인한 예약자의 특정 날짜 이전 (Inclusive) 예약 내역을 조회한다")
    void findAllByMemberAndReservationTimeDateLessThanEqual() {
        PageRequest pageRequest = PageRequest.of(0, 10, Sort.by("reservationTime.endTime").descending());
        Slice<Reservation> actual = reservations.findAllByMemberAndReservationTimeDateLessThanEqualAndReservationTimeEndTimeLessThanEqual(
                pobi,
                THE_DAY_AFTER_TOMORROW,
                TimeZoneUtils.convertToUTC(BE_PM_TWO_THREE_END_TIME_KST),
                pageRequest);

        assertThat(actual.getContent()).containsExactly(bePmTwoThreeByPobi, beFiveDaysAgoPmTwoThreeByPobi);
    }
}
