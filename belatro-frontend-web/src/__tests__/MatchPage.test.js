import React from 'react';
import { render, waitFor } from '@testing-library/react';
import MatchPage from '../pages/MatchPage';
import { MemoryRouter, Route, Routes } from 'react-router-dom';
import SockJS from 'sockjs-client';
import webstomp from 'webstomp-client';

//jeli dobra konekcija na websocket, subscriptioni i posalje li refresh


// mockovi
jest.mock('sockjs-client');
jest.mock('webstomp-client', () => ({
  over: jest.fn(),
}));

const mockConnect = jest.fn();
const mockSubscribe = jest.fn();
const mockSend = jest.fn();
const mockDisconnect = jest.fn();

const mockClient = {
  connect: mockConnect,
  subscribe: mockSubscribe,
  send: mockSend,
  disconnect: mockDisconnect,
  connected: true,
};

describe('MatchPage WebSocket Connection', () => {
  beforeEach(() => {
    SockJS.mockImplementation(() => ({}));
    webstomp.over.mockReturnValue(mockClient);
    localStorage.setItem('username', 'testUser');
    localStorage.setItem('token', 'fake-token');
    localStorage.setItem('userId', '123');
  }); //SockJS i webstomp vracaju mock client, local storage simulira fejk datu

  afterEach(() => {
    jest.clearAllMocks();
    localStorage.clear();
  });//clearea da svi testovi su fresh

  it('connects to WebSocket and subscribes correctly', async () => {
    mockConnect.mockImplementation((headers, onConnect) => {
      onConnect(); //simulira successfull konekciju
    });

    render(
      <MemoryRouter initialEntries={[{ pathname: '/match', state: { matchId: '1111222233334444' } }]}>
        <Routes>
          <Route path="/match" element={<MatchPage />} />
        </Routes>
      </MemoryRouter>
    );//rendera fejk MatchPage

    await waitFor(() => {
      expect(SockJS).toHaveBeenCalledWith(`${process.env.REACT_APP_API_URL}/ws?user=testUser`);
      expect(mockConnect).toHaveBeenCalled();
      expect(mockSubscribe).toHaveBeenCalledWith('/topic/games/1111222233334444', expect.any(Function));
      expect(mockSubscribe).toHaveBeenCalledWith('/user/queue/games/1111222233334444', expect.any(Function));
      expect(mockSend).toHaveBeenCalledWith('/app/games/1111222233334444/refresh', {}, '');
    });//provjerava da je ws konektiran na tocan url, subscriban na public i private kanal, i posalje refresh backendu
  });
});
