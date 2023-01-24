import styled from 'styled-components';

export const MapListContainer = styled.div`
  margin-top: 60px;
`;

export const MapListTitle = styled.div`
  font-size: 1.25rem;
  font-weight: bold;
`;

export const MapList = styled.div`
  margin: 28px 0;
`;

export const MapCreateButtonContainer = styled.div`
  margin: 36px 0;
  display: flex;
  justify-content: center;
`;

export const MapCreateButton = styled.button`
  background-color: ${({ theme }) => theme.white};
  border: 1px solid ${({ theme }) => theme.gray[400]};
  border-radius: 30px;
  padding: 12px 48px;
  font-size: 1.125rem;
  cursor: pointer;

  :hover {
    background-color: ${({ theme }) => theme.gray[100]};
  }
`;
