package com.woowacourse.zzimkkong.service;

import com.woowacourse.zzimkkong.domain.*;
import com.woowacourse.zzimkkong.dto.member.LoginUserEmail;
import com.woowacourse.zzimkkong.dto.reservation.*;
import com.woowacourse.zzimkkong.dto.slack.SlackResponse;
import com.woowacourse.zzimkkong.exception.map.NoSuchMapException;
import com.woowacourse.zzimkkong.exception.member.NoSuchMemberException;
import com.woowacourse.zzimkkong.exception.reservation.*;
import com.woowacourse.zzimkkong.exception.setting.MultipleSettingsException;
import com.woowacourse.zzimkkong.exception.setting.NoSettingAvailableException;
import com.woowacourse.zzimkkong.exception.space.NoSuchSpaceException;
import com.woowacourse.zzimkkong.infrastructure.datetime.TimeZoneUtils;
import com.woowacourse.zzimkkong.infrastructure.sharingid.SharingIdGenerator;
import com.woowacourse.zzimkkong.repository.MapRepository;
import com.woowacourse.zzimkkong.repository.MemberRepository;
import com.woowacourse.zzimkkong.repository.ReservationRepository;
import com.woowacourse.zzimkkong.service.strategy.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional
public class ReservationService {
    private final MapRepository maps;
    private final ReservationRepository reservations;
    private final MemberRepository members;
    private final SharingIdGenerator sharingIdGenerator;
    private final ReservationStrategies reservationStrategies;

    public ReservationService(
            final MapRepository maps,
            final ReservationRepository reservations,
            final MemberRepository members,
            final SharingIdGenerator sharingIdGenerator,
            final ReservationStrategies reservationStrategies) {
        this.maps = maps;
        this.reservations = reservations;
        this.members = members;
        this.sharingIdGenerator = sharingIdGenerator;
        this.reservationStrategies = reservationStrategies;
    }

    public ReservationCreateResponse saveReservation(
            final ReservationCreateDto reservationCreateDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationCreateDto.getReservationType());

        Long mapId = reservationCreateDto.getMapId();
        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);

        Reservation reservation = reservationStrategy.createReservation(map, reservationCreateDto);

        validateAvailability(reservation, new ExcludeReservationCreateStrategy());

        Reservation savedReservation = reservations.save(reservation);

        map.activateSharingMapId(sharingIdGenerator);

        return ReservationCreateResponse.of(savedReservation, map);
    }

    @Transactional(readOnly = true)
    public ReservationFindAllResponse findAllReservations(final ReservationFindAllDto reservationFindAllDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationFindAllDto.getReservationType());

        Long mapId = reservationFindAllDto.getMapId();
        LoginUserEmail loginUserEmail = reservationFindAllDto.getLoginUserEmail();

        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);
        reservationStrategy.validateManagerOfMap(map, loginUserEmail);

        List<Space> findSpaces = map.getSpaces();
        LocalDate date = reservationFindAllDto.getDate();
        List<Reservation> findReservations = getReservations(findSpaces, date);

        return ReservationFindAllResponse.of(findSpaces, findReservations, getLoginUser(loginUserEmail));
    }

    @Transactional(readOnly = true)
    public ReservationFindResponse findReservations(final ReservationFindDto reservationFindDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationFindDto.getReservationType());
        Long mapId = reservationFindDto.getMapId();
        LoginUserEmail loginUserEmail = reservationFindDto.getLoginUserEmail();

        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);
        reservationStrategy.validateManagerOfMap(map, loginUserEmail);

        Long spaceId = reservationFindDto.getSpaceId();
        LocalDate date = reservationFindDto.getDate();
        Space space = map.findSpaceById(spaceId)
                .orElseThrow(NoSuchSpaceException::new);
        List<Reservation> findReservations = getReservations(Collections.singletonList(space), date);

        return ReservationFindResponse.from(findReservations, getLoginUser(loginUserEmail));
    }

    @Transactional(readOnly = true)
    public ReservationResponse findReservation(final ReservationAuthenticationDto reservationAuthenticationDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationAuthenticationDto.getReservationType());
        Long mapId = reservationAuthenticationDto.getMapId();
        LoginUserEmail loginUserEmail = reservationAuthenticationDto.getLoginUserEmail();

        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);
        reservationStrategy.validateManagerOfMap(map, loginUserEmail);

        Long spaceId = reservationAuthenticationDto.getSpaceId();
        validateSpaceExistence(map, spaceId);

        Long reservationId = reservationAuthenticationDto.getReservationId();
        Reservation reservation = reservations
                .findById(reservationId)
                .orElseThrow(NoSuchReservationException::new);
        reservationStrategy.validateOwnerOfReservation(
                reservation,
                reservationAuthenticationDto.getPassword(),
                loginUserEmail);

        return ReservationResponse.from(reservation, getLoginUser(loginUserEmail));
    }

    @Transactional(readOnly = true)
    public ReservationInfiniteScrollResponse findUpcomingReservations(final LoginUserEmail loginUserEmail, final Pageable pageable) {
        Member member = members.findByEmail(loginUserEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        LocalDateTime now = LocalDateTime.now();
        Slice<Reservation> reservationSlice = reservations.findAllByMemberAndReservationTimeDateGreaterThanEqualAndReservationTimeEndTimeGreaterThanEqual(
                member,
                TimeZoneUtils.convertTo(now, ServiceZone.KOREA).toLocalDate(),
                now,
                pageable);
        List<Reservation> upcomingReservations = reservationSlice.getContent()
                .stream()
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .peek(reservation -> reservation.getSpace().getMap().activateSharingMapId(sharingIdGenerator))
                .collect(Collectors.toList());

        return ReservationInfiniteScrollResponse.of(upcomingReservations, reservationSlice.hasNext(), pageable.getPageNumber());
    }

    @Transactional(readOnly = true)
    public ReservationInfiniteScrollResponse findPreviousReservations(final LoginUserEmail loginUserEmail, final Pageable pageable) {
        Member member = members.findByEmail(loginUserEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);

        LocalDateTime now = LocalDateTime.now();
        Slice<Reservation> reservationSlice = reservations.findAllByMemberAndReservationTimeDateLessThanEqualAndReservationTimeEndTimeLessThanEqual(
                member,
                TimeZoneUtils.convertTo(now, ServiceZone.KOREA).toLocalDate(),
                now,
                pageable);

        List<Reservation> previousReservations = reservationSlice.getContent()
                .stream()
                .sorted(Comparator.comparing(Reservation::getStartTime).reversed())
                .peek(reservation -> reservation.getSpace().getMap().activateSharingMapId(sharingIdGenerator))
                .collect(Collectors.toList());

        return ReservationInfiniteScrollResponse.of(previousReservations, reservationSlice.hasNext(), pageable.getPageNumber());
    }

    @Transactional(readOnly = true)
    public ReservationInfiniteScrollResponse findUpcomingNonLoginReservations(
            final String userName,
            final LocalDateTime searchStartTime,
            final Pageable pageable) {
        Slice<Reservation> reservationSlice = reservations.findAllByUserNameAndReservationTimeDateGreaterThanEqualAndReservationTimeEndTimeGreaterThanEqualAndMemberIsNull(
                userName,
                TimeZoneUtils.convertTo(searchStartTime, ServiceZone.KOREA).toLocalDate(),
                searchStartTime,
                pageable);

        List<Reservation> upcomingNonLoginReservations = reservationSlice.getContent()
                .stream()
                .sorted(Comparator.comparing(Reservation::getStartTime))
                .peek(reservation -> reservation.getSpace().getMap().activateSharingMapId(sharingIdGenerator))
                .collect(Collectors.toList());

        return ReservationInfiniteScrollResponse.of(upcomingNonLoginReservations, reservationSlice.hasNext(), pageable.getPageNumber());
    }

    public SlackResponse updateReservation(final ReservationUpdateDto reservationUpdateDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationUpdateDto.getReservationType());
        Long mapId = reservationUpdateDto.getMapId();
        LoginUserEmail loginUserEmail = reservationUpdateDto.getLoginUserEmail();

        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);
        reservationStrategy.validateManagerOfMap(map, loginUserEmail);

        Long reservationId = reservationUpdateDto.getReservationId();
        Reservation reservation = reservations.findById(reservationId)
                .orElseThrow(NoSuchReservationException::new);
        reservationStrategy.validateOwnerOfReservation(reservation, reservationUpdateDto.getPassword(), loginUserEmail);

        Reservation updateReservation = reservationStrategy.createReservation(map, reservationUpdateDto);

        validateAvailability(updateReservation, new ExcludeReservationUpdateStrategy(reservation));

        reservation.update(updateReservation);

        map.activateSharingMapId(sharingIdGenerator);

        return SlackResponse.of(reservation, map);
    }

    public SlackResponse deleteReservation(final ReservationAuthenticationDto reservationAuthenticationDto) {
        ReservationStrategy reservationStrategy = reservationStrategies.getStrategyByReservationType(reservationAuthenticationDto.getReservationType());
        Long mapId = reservationAuthenticationDto.getMapId();
        LoginUserEmail loginUserEmail = reservationAuthenticationDto.getLoginUserEmail();

        Map map = maps.findByIdFetch(mapId)
                .orElseThrow(NoSuchMapException::new);
        reservationStrategy.validateManagerOfMap(map, loginUserEmail);

        Long spaceId = reservationAuthenticationDto.getSpaceId();
        validateSpaceExistence(map, spaceId);

        Long reservationId = reservationAuthenticationDto.getReservationId();
        Reservation reservation = reservations
                .findById(reservationId)
                .orElseThrow(NoSuchReservationException::new);
        reservationStrategy.validateOwnerOfReservation(
                reservation,
                reservationAuthenticationDto.getPassword(),
                loginUserEmail);

        if (!reservationStrategy.isManager()) {
            validateDeletability(reservation);
        }

        reservations.delete(reservation);

        map.activateSharingMapId(sharingIdGenerator);

        return SlackResponse.of(reservation, map);
    }

    private Member getLoginUser(final LoginUserEmail loginUserEmail) {
        if (!loginUserEmail.exists()) {
            return Member.builder().build();
        }
        return members.findByEmail(loginUserEmail.getEmail())
                .orElseThrow(NoSuchMemberException::new);
    }

    private void validateDeletability(final Reservation reservation) {
        LocalDateTime now = LocalDateTime.now();
        if (reservation.isInUse(now)) {
            throw new DeleteReservationInUseException();
        }

        if (reservation.isExpired(now)) {
            throw new DeleteExpiredReservationException();
        }
    }

    private void validateAvailability(
            final Reservation reservation,
            final ExcludeReservationStrategy excludeReservationStrategy) {
        Space space = reservation.getSpace();
        validateSpaceSetting(space, reservation);

        List<Reservation> reservationsOnDate = getReservations(
                Collections.singletonList(space),
                reservation.getDate());
        excludeReservationStrategy.apply(space, reservationsOnDate);

        validateTimeConflicts(reservation, reservationsOnDate);
    }

    private void validateSpaceSetting(final Space space, final Reservation reservation) {
        TimeSlot timeSlot = reservation.getTimeSlot();
        DayOfWeek dayOfWeek = reservation.getDayOfWeek();

        Settings relevantSettings = space.getRelevantSettings(timeSlot, dayOfWeek);

        if (relevantSettings.isEmpty()) {
            space.getSpaceSettings().flatten();
            throw new NoSettingAvailableException(space);
        }

        // TODO: 추후 N부제 -> 예약 유도로 넘어갈 때 이부분이 제거되어야함 - 여러 조건에 걸치면 유도하는 식으로 로직이 변경되어야 하기 때문
        if (relevantSettings.haveMultipleSettings()) {
            throw new MultipleSettingsException(relevantSettings);
        }

        if (relevantSettings.cannotAcceptDueToAvailableTime(timeSlot, dayOfWeek)) {
            throw new InvalidStartEndTimeException(relevantSettings.getUnavailableTimeSlots(dayOfWeek), timeSlot);
        }

        // TODO: 2023/02/09 기준, 예약은 하나의 세팅만 걸쳐야한다
        Setting setting = relevantSettings.getSettings().get(0);

        if (setting.cannotAcceptDueToTimeUnit(timeSlot)) {
            throw new InvalidTimeUnitException();
        }

        if (setting.cannotAcceptDueToMinimumTimeUnit(timeSlot)) {
            throw new InvalidMinimumDurationTimeException();
        }

        if (setting.cannotAcceptDueToMaximumTimeUnit(timeSlot)) {
            throw new InvalidMaximumDurationTimeException();
        }

        if (space.isUnableToReserve()) {
            throw new InvalidReservationEnableException();
        }
    }

    private void validateTimeConflicts(
            final Reservation reservation,
            final List<Reservation> reservationsOnDate) {
        for (Reservation existingReservation : reservationsOnDate) {
            if (existingReservation.hasConflictWith(reservation)) {
                throw new ReservationAlreadyExistsException();
            }
        }
    }

    private List<Reservation> getReservations(final Collection<Space> findSpaces, final LocalDate date) {
        List<Long> spaceIds = findSpaces.stream()
                .map(Space::getId)
                .collect(Collectors.toList());

        return reservations.findAllBySpaceIdInAndReservationTimeDate(spaceIds, date);
    }

    private void validateSpaceExistence(final Map map, final Long spaceId) {
        if (map.doesNotHaveSpaceId(spaceId)) {
            throw new NoSuchSpaceException();
        }
    }
}
