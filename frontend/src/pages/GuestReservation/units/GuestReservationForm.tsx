import dayjs from 'dayjs';
import React, { ChangeEventHandler, useMemo } from 'react';
import { ReactComponent as CalendarIcon } from 'assets/svg/calendar.svg';
import Input from 'components/Input/Input';
import TimePicker, { Step } from 'components/TimePicker/TimePicker';
import DATE from 'constants/date';
import MESSAGE from 'constants/message';
import REGEXP from 'constants/regexp';
import RESERVATION from 'constants/reservation';
import SPACE from 'constants/space';
import useInputs from 'hooks/useInputs';
import useScrollToTop from 'hooks/useScrollToTop';
import useTimePicker from 'hooks/useTimePicker';
import { Reservation, Space } from 'types/common';
import {
  convertSettingTimeToMinutes,
  convertTimeToMinutes,
  formatTimeWithSecond,
  isPastDate,
} from 'utils/datetime';
import useSettingSummary from '../../../hooks/query/useSettingSummary';
import { EditGuestReservationParams } from '../GuestReservation';
import * as Styled from './GuestReservationForm.styles';

interface Props {
  isEditMode: boolean;
  mapId: number;
  spaceId: number;
  space: Pick<Space, 'settings'>;
  reservation?: Reservation;
  date: string;
  onChangeDate: ChangeEventHandler<HTMLInputElement>;
  onSubmit: (
    event: React.FormEvent<HTMLFormElement>,
    { reservation, reservationId }: EditGuestReservationParams
  ) => void;
}

interface Form {
  name: string;
  description: string;
  password: string;
}

const GuestReservationForm = ({
  isEditMode,
  mapId,
  spaceId,
  space,
  date,
  reservation,
  onSubmit,
  onChangeDate,
}: Props): JSX.Element => {
  useScrollToTop();

  const reservationTimeStep = useMemo(() => {
    const startTime = convertTimeToMinutes(
      reservation ? new Date(reservation.startDateTime) : new Date()
    );
    const endTime = convertTimeToMinutes(
      reservation ? new Date(reservation.endDateTime) : new Date()
    );

    return Math.min(
      ...space.settings
        .filter((setting) => {
          const settingStartTime = convertSettingTimeToMinutes(setting.settingStartTime);
          const settingEndTime = convertSettingTimeToMinutes(setting.settingEndTime);

          return (
            (settingStartTime < startTime && settingEndTime < startTime) ||
            (settingStartTime < endTime && settingEndTime > endTime)
          );
        })
        .map(({ reservationTimeUnit }) => reservationTimeUnit),
      SPACE.RESERVATION.MIN_STEP
    );
  }, [reservation, space.settings]);

  const { range, selectedTime, onClick, onChange, onCloseOptions } = useTimePicker({
    step: reservationTimeStep as Step,
    initialStartTime: !!reservation ? new Date(reservation.startDateTime) : undefined,
    initialEndTime: !!reservation ? new Date(reservation.endDateTime) : undefined,
  });

  const getSettingsSummary = useSettingSummary(
    {
      mapId,
      spaceId,
      selectedDateTime: `${date}T${formatTimeWithSecond(range.start ?? dayjs().tz())}${
        DATE.TIMEZONE_OFFSET
      }`,
      settingViewType: 'FLAT',
    },
    {}
  );
  const settingsSummary = getSettingsSummary.data?.data?.summary ?? '';

  const [{ name, description, password }, onChangeForm] = useInputs<Form>({
    name: reservation?.name ?? '',
    description: reservation?.description ?? '',
    password: '',
  });

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault();

    if (range.start === null || range.end === null) return;

    const startDateTime = `${date}T${formatTimeWithSecond(range.start)}${DATE.TIMEZONE_OFFSET}`;
    const endDateTime = `${date}T${formatTimeWithSecond(range.end)}${DATE.TIMEZONE_OFFSET}`;

    onSubmit(event, {
      reservation: {
        startDateTime,
        endDateTime,
        password,
        name,
        description,
      },
      reservationId: reservation?.id,
    });
  };
  return (
    <Styled.ReservationForm onSubmit={handleSubmit}>
      <Styled.Section>
        <Styled.InputWrapper>
          <Input
            label="이름"
            name="name"
            value={name}
            onChange={onChangeForm}
            maxLength={RESERVATION.NAME.MAX_LENGTH}
            autoFocus
            required
          />
        </Styled.InputWrapper>
        <Styled.InputWrapper>
          <Input
            label="사용 목적"
            name="description"
            value={description}
            onChange={onChangeForm}
            maxLength={RESERVATION.DESCRIPTION.MAX_LENGTH}
            required
          />
        </Styled.InputWrapper>
        <Styled.InputWrapper>
          <Input
            type="date"
            name="date"
            label="날짜"
            icon={<CalendarIcon />}
            value={date}
            min={DATE.MIN_DATE_STRING}
            max={DATE.MAX_DATE_STRING}
            onChange={onChangeDate}
            required
          />
        </Styled.InputWrapper>
        <Styled.InputWrapper>
          <TimePicker
            label="예약시간"
            range={range}
            step={reservationTimeStep as Step}
            selectedTime={selectedTime}
            onClick={onClick}
            onChange={onChange}
            onCloseOptions={onCloseOptions}
          />
          <Styled.SettingSummaryWrapper>
            <Styled.SettingSummary fontWeight="bold">예약 가능 시간</Styled.SettingSummary>
            <Styled.SettingSummary>{settingsSummary}</Styled.SettingSummary>
          </Styled.SettingSummaryWrapper>
        </Styled.InputWrapper>
        <Styled.InputWrapper>
          <Input
            type="password"
            label="비밀번호"
            name="password"
            value={password}
            onChange={onChangeForm}
            minLength={RESERVATION.PASSWORD.MIN_LENGTH}
            maxLength={RESERVATION.PASSWORD.MAX_LENGTH}
            pattern={REGEXP.RESERVATION_PASSWORD.source}
            inputMode="numeric"
            message={MESSAGE.RESERVATION.PASSWORD_MESSAGE}
            required
          />
        </Styled.InputWrapper>
      </Styled.Section>
      <Styled.ButtonWrapper>
        <Styled.ReservationButton
          fullWidth
          variant="primary"
          size="large"
          disabled={isPastDate(new Date(date))}
        >
          {isEditMode ? MESSAGE.RESERVATION.EDIT : MESSAGE.RESERVATION.CREATE}
        </Styled.ReservationButton>
      </Styled.ButtonWrapper>
    </Styled.ReservationForm>
  );
};

export default GuestReservationForm;
