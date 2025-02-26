import { AxiosError } from 'axios';
import { FormEventHandler, useMemo, useState } from 'react';
import { useMutation } from 'react-query';
import { deletePreset, postPreset } from 'api/presets';
import { ReactComponent as DeleteIcon } from 'assets/svg/delete.svg';
import Button from 'components/Button/Button';
import IconButton from 'components/IconButton/IconButton';
import Select from 'components/Select/Select';
import MESSAGE from 'constants/message';
import THROW_ERROR from 'constants/throwError';
import usePresets from 'hooks/query/usePreset';
import useFormContext from 'hooks/useFormContext';
import useInput from 'hooks/useInput';
import { Preset as PresetType } from 'types/common';
import { ErrorResponse } from 'types/response';
import { SpaceFormContext } from '../providers/SpaceFormProvider';
import * as Styled from './Preset.styles';
import PresetNameModal from './PresetNameModal';

interface CreateResponseHeaders {
  location: string;
}

const Preset = (): JSX.Element => {
  const {
    values,
    setValues,
    selectedPresetId,
    setSelectedPresetId,
    getRequestValues,
    selectedSettingIndex,
  } = useFormContext(SpaceFormContext);
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [presetName, onChangePresetName] = useInput('');

  const getPresets = usePresets();
  const presets = useMemo(
    () => getPresets.data?.data?.presets ?? [],
    [getPresets.data?.data?.presets]
  );

  const createPreset = useMutation(postPreset, {
    onSuccess: (response) => {
      const { location } = response.headers as CreateResponseHeaders;
      const newPresetId = Number(location.split('/').pop() ?? '');

      setSelectedPresetId(newPresetId);
      setIsModalOpen(false);
      getPresets.refetch();
    },
    onError: (error: AxiosError<ErrorResponse>) => {
      alert(error.response?.data.message ?? MESSAGE.MANAGER_SPACE.ADD_PRESET_UNEXPECTED_ERROR);
    },
  });

  const removePreset = useMutation(deletePreset, {
    onError: (error: AxiosError<ErrorResponse>) => {
      alert(error.response?.data.message ?? MESSAGE.MANAGER_SPACE.DELETE_PRESET_UNEXPECTED_ERROR);
    },
  });

  const handleSelectPreset = (id: number | null) => {
    if (id === null) return;

    const selectedPreset = presets.find((preset) => preset.id === id) ?? null;

    if (selectedPreset === null) throw new Error(THROW_ERROR.NOT_EXIST_PRESET);

    const { id: presetId, name, ...rest } = selectedPreset;

    setValues({
      ...values,
      settings: values.settings.map((setting, index) => {
        if (index === selectedSettingIndex) {
          return { ...rest, priorityOrder: 0 };
        }

        return setting;
      }),
    });

    setSelectedPresetId(id);
  };

  const handleAddPreset = () => {
    setIsModalOpen(true);
  };

  const handleSubmitPreset: FormEventHandler<HTMLFormElement> = (event) => {
    event.preventDefault();
    event.stopPropagation();

    const requestValues = getRequestValues();

    createPreset.mutate({
      name: presetName,
      preset: requestValues.space.settings[selectedSettingIndex],
    });
  };

  const handleDeletePreset = (event: React.MouseEvent<HTMLButtonElement>, id: PresetType['id']) => {
    event.stopPropagation();

    if (!window.confirm(MESSAGE.MANAGER_SPACE.DELETE_PRESET_CONFIRM)) return;

    removePreset.mutate({ id });
  };

  const presetOptions = presets.map(({ id, name }) => ({
    value: `${id}`,
    title: name,
    children: (
      <Styled.PresetOption>
        <Styled.PresetName>{name}</Styled.PresetName>
        <IconButton
          type="button"
          size="small"
          onClick={(event) => handleDeletePreset(event, Number(id))}
        >
          <DeleteIcon width="100%" height="100%" />
        </IconButton>
      </Styled.PresetOption>
    ),
  }));

  return (
    <>
      <Styled.PresetSelect>
        <Styled.PresetSelectWrapper>
          <Select
            name="preset"
            label="프리셋 선택"
            options={presetOptions}
            value={`${selectedPresetId ?? ''}`}
            onChange={(id) => handleSelectPreset(Number(id))}
          />
        </Styled.PresetSelectWrapper>
        <Button type="button" onClick={handleAddPreset}>
          추가
        </Button>
      </Styled.PresetSelect>

      <PresetNameModal
        open={isModalOpen}
        onClose={() => setIsModalOpen(false)}
        presetName={presetName}
        onChange={onChangePresetName}
        onSubmit={handleSubmitPreset}
        errorMessage={createPreset.error?.response?.data.message}
      />
    </>
  );
};

export default Preset;
