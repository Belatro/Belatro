import axios from 'axios';
import { createLobby } from '../services/lobbyService';

//mockani axios provjerava jeli radi create lobby i koristi axios kako treba

jest.mock('axios');

describe('Lobby Service', () => {
  it('creates a lobby successfully', async () => {
    const mockLobbyData = { name: 'Test Lobby', password: '1234' };
    const mockResponse = { data: { id: 1, ...mockLobbyData } };

    axios.post.mockResolvedValueOnce(mockResponse);

    const result = await createLobby(mockLobbyData); //calla create lobby sa fejk datom od gore

    expect(result).toEqual(mockResponse.data); // provjerava jeli returnalo response datu
    expect(axios.post).toHaveBeenCalledWith('http://localhost:8080/lobbies', mockLobbyData);// provjeravaja axios callan sa pravin url i daton
  });
});
