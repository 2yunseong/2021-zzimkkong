package com.woowacourse.zzimkkong.service;

import com.woowacourse.zzimkkong.domain.*;
import com.woowacourse.zzimkkong.dto.member.LoginUserEmail;
import com.woowacourse.zzimkkong.dto.reservation.ReservationCreateUpdateWithPasswordRequest;
import com.woowacourse.zzimkkong.dto.space.*;
import com.woowacourse.zzimkkong.dto.map.NoAuthorityOnMapException;
import com.woowacourse.zzimkkong.exception.map.NoSuchMapException;
import com.woowacourse.zzimkkong.exception.space.NoSuchSpaceException;
import com.woowacourse.zzimkkong.exception.space.ReservationExistOnSpaceException;
import com.woowacourse.zzimkkong.infrastructure.datetime.TimeZoneUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import static com.woowacourse.zzimkkong.Constants.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

class SpaceServiceTest extends ServiceTest {
    @Autowired
    private SpaceService spaceService;

    private final SettingRequest settingRequest = new SettingRequest(
            BE_AVAILABLE_START_TIME,
            BE_AVAILABLE_END_TIME,
            BE_RESERVATION_TIME_UNIT.getMinutes(),
            BE_RESERVATION_MINIMUM_TIME_UNIT.getMinutes(),
            BE_RESERVATION_MAXIMUM_TIME_UNIT.getMinutes(),
            EnabledDayOfWeekDto.from(BE_ENABLED_DAY_OF_WEEK),
            0
    );

    private final SpaceCreateUpdateRequest spaceCreateUpdateRequest = new SpaceCreateUpdateRequest(
            BE_NAME,
            BE_COLOR,
            SPACE_DRAWING,
            MAP_SVG,
            BE_RESERVATION_ENABLE,
            List.of(settingRequest)
    );

    private final SpaceCreateUpdateRequest updateSpaceCreateUpdateRequest = new SpaceCreateUpdateRequest(
            BE_NAME,
            "#FFCCE5",
            SPACE_DRAWING,
            MAP_SVG,
            BE_RESERVATION_ENABLE,
            List.of(settingRequest)
    );

    private Member pobi;
    private Member sakjung;
    private LoginUserEmail pobiEmail;
    private LoginUserEmail sakjungEmail;
    private Map luther;
    private Space be;
    private Space fe;

    private Long lutherId;
    private Long beId;
    private Long noneExistingMapId;
    private Long noneExistingSpaceId;

    @BeforeEach
    void setUp() {
        pobi = Member.builder()
                .email(EMAIL)
                .userName(POBI)
                .emoji(ProfileEmoji.MAN_DARK_SKIN_TONE_TECHNOLOGIST)
                .password(PW)
                .organization(ORGANIZATION)
                .build();
        sakjung = Member.builder()
                .email(NEW_EMAIL)
                .userName(POBI)
                .emoji(ProfileEmoji.MAN_DARK_SKIN_TONE_TECHNOLOGIST)
                .password(PW)
                .organization(ORGANIZATION)
                .build();
        pobiEmail = LoginUserEmail.from(EMAIL);
        sakjungEmail = LoginUserEmail.from(NEW_EMAIL);
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

        lutherId = luther.getId();
        beId = be.getId();
        noneExistingMapId = luther.getId() + 1;
        noneExistingSpaceId = (long) (luther.getSpaces().size() + 1);
    }

    @Test
    @DisplayName("공간 생성 요청 시, 공간을 생성한다.")
    void save() {
        // given
        Setting setting = Setting.builder()
                .settingTimeSlot(TimeSlot.of(
                        BE_AVAILABLE_START_TIME,
                        BE_AVAILABLE_END_TIME))
                .reservationTimeUnit(BE_RESERVATION_TIME_UNIT)
                .reservationMinimumTimeUnit(BE_RESERVATION_MINIMUM_TIME_UNIT)
                .reservationMaximumTimeUnit(BE_RESERVATION_MAXIMUM_TIME_UNIT)
                .enabledDayOfWeek(BE_ENABLED_DAY_OF_WEEK)
                .priorityOrder(0)
                .build();

        Space newSpace = Space.builder()
                .id(3L)
                .name("새로운 공간")
                .map(luther)
                .area(SPACE_DRAWING)
                .reservationEnable(BE_RESERVATION_ENABLE)
                .spaceSettings(Settings.toPrioritizedSettings(List.of(setting)))
                .build();

        given(maps.findById(anyLong()))
                .willReturn(Optional.of(luther));
        given(spaces.save(any(Space.class)))
                .willReturn(newSpace);

        // when
        SpaceCreateResponse spaceCreateResponse = spaceService.saveSpace(luther.getId(), spaceCreateUpdateRequest, pobiEmail);

        // then
        assertThat(spaceCreateResponse.getId()).isEqualTo(newSpace.getId());
    }

    @Test
    @DisplayName("공간 생성 요청 시, 맵이 존재하지 않는다면 예외가 발생한다.")
    void saveNotExistMapException() {
        // given
        given(maps.findById(anyLong()))
                .willReturn(Optional.empty());

        // when, then
        assertThatThrownBy(() -> spaceService.saveSpace(noneExistingMapId, spaceCreateUpdateRequest, pobiEmail))
                .isInstanceOf(NoSuchMapException.class);
    }

    @Test
    @DisplayName("공간 생성 요청 시, 맵에 대한 권한이 없다면 예외가 발생한다.")
    void saveNoAuthorityOnMapException() {
        // given
        given(maps.findById(anyLong()))
                .willReturn(Optional.of(luther));
        given(spaces.save(any(Space.class)))
                .willReturn(be);

        // when, then
        assertThatThrownBy(() -> spaceService.saveSpace(lutherId, spaceCreateUpdateRequest, sakjungEmail))
                .isInstanceOf(NoAuthorityOnMapException.class);
    }

    @Test
    @DisplayName("공간 조회 시, spaceId를 가진 공간이 있다면 조회한다.")
    void find() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        SpaceFindDetailResponse actual = spaceService.findSpace(luther.getId(), be.getId(), pobiEmail);

        // then
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(SpaceFindDetailResponse.from(be));
    }

    @Test
    @DisplayName("공간 조회 시, spaceId에 맞는 공간이 없다면 예외를 발생시킨다.")
    void findFail() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when, then
        assertThatThrownBy(() -> spaceService.findSpace(lutherId, noneExistingSpaceId, pobiEmail))
                .isInstanceOf(NoSuchSpaceException.class);
    }

    @Test
    @DisplayName("공간 조회 시, 공간 관리자가 아니라면 예외를 발생시킨다.")
    void findNoAuthorityOnMap() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(spaces.findById(anyLong()))
                .willReturn(Optional.of(be));

        // when, then
        assertThatThrownBy(() -> spaceService.findSpace(lutherId, beId, sakjungEmail))
                .isInstanceOf(NoAuthorityOnMapException.class);
    }

    @Test
    @DisplayName("전체 공간을 조회한다.")
    void findAll() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        SpaceFindAllResponse actual = spaceService.findAllSpace(luther.getId(), pobiEmail);

        // then
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(SpaceFindAllResponse.from(List.of(be, fe)));
    }

    @Test
    @DisplayName("공간 전체 조회시, 공간 관리자가 아니라면 예외를 발생시킨다.")
    void findAllNoAuthorityOnMap() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when, then
        assertThatThrownBy(() -> spaceService.findAllSpace(lutherId, sakjungEmail))
                .isInstanceOf(NoAuthorityOnMapException.class);
    }

    @Test
    @DisplayName("예약자 전체 공간을 조회한다.")
    void findAllGuest() {
        // given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // when
        SpaceFindAllResponse actual = spaceService.findAllSpace(luther.getId());

        // then
        assertThat(actual).usingRecursiveComparison()
                .isEqualTo(SpaceFindAllResponse.from(List.of(be, fe)));
    }

    @Test
    @DisplayName("공간 수정 요청 시, 해당 공간에 대한 권한이 없으면 수정할 수 없다.")
    void updateNoAuthorityException() {
        // given, when
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));

        // then
        assertThatThrownBy(() -> spaceService.updateSpace(
                lutherId,
                beId,
                updateSpaceCreateUpdateRequest,
                sakjungEmail))
                .isInstanceOf(NoAuthorityOnMapException.class);
    }

    @Test
    @DisplayName("공간 삭제 요청이 옳다면 삭제한다.")
    void deleteReservation() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.existsBySpaceIdAndReservationTimeEndTimeAfter(anyLong(), any(LocalDateTime.class)))
                .willReturn(false);
        SpaceDeleteRequest spaceDeleteRequest = new SpaceDeleteRequest(MAP_SVG);

        //then
        assertDoesNotThrow(() -> spaceService.deleteSpace(luther.getId(), be.getId(), spaceDeleteRequest, pobiEmail));
    }

    @Test
    @DisplayName("공간 삭제 요청 시, 해당 맵의 관리자가 아니라면 오류가 발생한다.")
    void deleteNoAuthorityException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        SpaceDeleteRequest spaceDeleteRequest = new SpaceDeleteRequest(MAP_SVG);

        //then
        assertThatThrownBy(() -> spaceService.deleteSpace(lutherId, beId, spaceDeleteRequest, sakjungEmail))
                .isInstanceOf(NoAuthorityOnMapException.class);
    }

    @Test
    @DisplayName("공간 삭제 요청 시, 공간이 존재하지 않는다면 오류가 발생한다.")
    void deleteNoSuchSpaceException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        SpaceDeleteRequest spaceDeleteRequest = new SpaceDeleteRequest(MAP_SVG);

        //then
        assertThatThrownBy(() -> spaceService.deleteSpace(lutherId, noneExistingSpaceId, spaceDeleteRequest, pobiEmail))
                .isInstanceOf(NoSuchSpaceException.class);
    }

    @Test
    @DisplayName("공간 삭제 요청 시, 해당 공간에 예약이 존재한다면 오류가 발생한다.")
    void deleteReservationExistException() {
        //given
        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.existsBySpaceIdAndReservationTimeEndTimeAfter(anyLong(), any(LocalDateTime.class)))
                .willReturn(true);
        SpaceDeleteRequest spaceDeleteRequest = new SpaceDeleteRequest(MAP_SVG);

        assertThatThrownBy(() -> spaceService.deleteSpace(lutherId, beId, spaceDeleteRequest, pobiEmail))
                .isInstanceOf(ReservationExistOnSpaceException.class);
    }

    @Test
    @DisplayName("특정 맵의 전체 공간 사용 가능 여부 조회 시, 주어진 시간대역에 대한 총 예약 현황을 고려하여 공간들의 사용가능 여부를 반환한다")
    void findAllSpaceAvailability() {
        // given
        Reservation beAmTenEleven = Reservation.builder()
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

        Reservation bePmOneTwo = Reservation.builder()
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

        given(maps.findByIdFetch(anyLong()))
                .willReturn(Optional.of(luther));
        given(reservations.findAllBySpaceIdInAndReservationTimeDate(anyCollection(), any()))
                .willReturn(List.of(beAmTenEleven, bePmOneTwo));

        // when
        ZonedDateTime tenThirtyKST = THE_DAY_AFTER_TOMORROW.atTime(10, 30).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone()));
        ZonedDateTime elevenThirtyKST = THE_DAY_AFTER_TOMORROW.atTime(11, 30).atZone(ZoneId.of(ServiceZone.KOREA.getTimeZone()));
        SpaceFindAllAvailabilityResponse actualResult = spaceService.findAllSpaceAvailability(
                1L,
                TimeZoneUtils.convertToUTC(tenThirtyKST),
                TimeZoneUtils.convertToUTC(elevenThirtyKST));

        // then
        SpaceFindAllAvailabilityResponse expectedResult = SpaceFindAllAvailabilityResponse.of(
                1L,
                List.of(be, fe),
                Set.of(be));

        assertThat(actualResult).usingRecursiveComparison().isEqualTo(expectedResult);
    }
}
