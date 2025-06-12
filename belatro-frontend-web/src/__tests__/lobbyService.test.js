import axios from 'axios';
import { createLobby } from '../services/lobbyService';

//mockani axios provjerava jeli radi create lobby i koristi axios kako treba

jest.mock('axios');

describe('Lobby Service', () => {
  it('creates a lobby successfully', async () => {
    const mockLobbyData = { name: 'Test Lobby', password: '1234' };
    const mockResponse = { data: { id: 1, ...mockLobbyData } };

    axios.post.mockResolvedValueOnce(mockResponse);

    const result = await createLobby(mockLobbyData); //calla create lobby sa fejk daton od gore

    expect(result).toEqual(mockResponse.data); // provjerava jeli returnalo response datu
    expect(axios.post).toHaveBeenCalledWith(`${process.env.REACT_APP_API_URL}/lobbies`, mockLobbyData);// provjerava axios callan sa pravin url i daton
  });
});
