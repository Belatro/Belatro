import { lobbyService } from './lobbyService';
import axiosInstance from './api';
import { LobbyDTO, CreateLobbyDTO, JoinLobbyRequestDTO, TeamSwitchRequestDTO } from './lobbyService';

jest.mock('./api');
const mockedAxios = axiosInstance as jest.Mocked<typeof axiosInstance>;

describe('lobbyService', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  const mockLobby: LobbyDTO = {
    id: '1',
    name: 'Test Lobby',
    hostUser: { id: 'host1', username: 'testhost' },
    teamAPlayers: [],
    teamBPlayers: [],
    gameMode: 'classic',
    status: 'waiting',
    privateLobby: false
  };

  it('fetches all open lobbies', async () => {
    mockedAxios.get.mockResolvedValue({ data: [mockLobby] });
    const response = await lobbyService.getAllOpenLobbies();
    expect(mockedAxios.get).toHaveBeenCalledWith('/lobbies/open');
    expect(response.data).toEqual([mockLobby]);
  });

  it('creates a new lobby', async () => {
    const newLobby: CreateLobbyDTO = {
      name: 'New Lobby',
      hostUser: { id: 'host1', username: 'testhost' },
      gameMode: 'classic',
      privateLobby: false
    };
    
    mockedAxios.post.mockResolvedValue({ data: mockLobby });
    const response = await lobbyService.createLobby(newLobby);
    expect(mockedAxios.post).toHaveBeenCalledWith('/lobbies', newLobby);
    expect(response.data).toEqual(mockLobby);
  });

  it('joins an existing lobby', async () => {
    const joinRequest: JoinLobbyRequestDTO = {
      lobbyId: '1',
      userId: 'user1',
      password: 'testpass'
    };
    
    mockedAxios.post.mockResolvedValue({ data: mockLobby });
    const response = await lobbyService.joinLobby(joinRequest);
    expect(mockedAxios.post).toHaveBeenCalledWith('/lobbies/join', joinRequest);
    expect(response.data).toEqual(mockLobby);
  });

  it('fetches lobby details', async () => {
    mockedAxios.get.mockResolvedValue({ data: mockLobby });
    const response = await lobbyService.getLobbyDetails('1');
    expect(mockedAxios.get).toHaveBeenCalledWith('/lobbies/1');
    expect(response.data).toEqual(mockLobby);
  });

  it('starts a match', async () => {
    mockedAxios.post.mockResolvedValue({ data: { ...mockLobby, status: 'in-progress' } });
    const response = await lobbyService.startMatch('1');
    expect(mockedAxios.post).toHaveBeenCalledWith('/lobbies/1/start-match');
    expect(response.data.status).toBe('in-progress');
  });

  it('switches teams', async () => {
    const switchRequest: TeamSwitchRequestDTO = {
      lobbyId: '1',
      userId: 'user1',
      targetTeam: 'A'
    };
    
    mockedAxios.post.mockResolvedValue({ data: mockLobby });
    const response = await lobbyService.switchTeam(switchRequest);
    expect(mockedAxios.post).toHaveBeenCalledWith('/lobbies/switchTeam', switchRequest);
    expect(response.data).toEqual(mockLobby);
  });

  it('leaves a lobby', async () => {
    mockedAxios.patch.mockResolvedValue({ data: {} });
    const response = await lobbyService.leaveLobby('1', 'user1');
    expect(mockedAxios.patch).toHaveBeenCalledWith('/lobbies/1/leave', { username: 'user1' });
    expect(response.data).toEqual({});
  });

  it('deletes a lobby', async () => {
    mockedAxios.delete.mockResolvedValue({ data: {} });
    const response = await lobbyService.deleteLobby('1');
    expect(mockedAxios.delete).toHaveBeenCalledWith('/lobbies/1');
    expect(response.data).toEqual({});
  });

  it('updates a lobby', async () => {
    const updatedLobby: LobbyDTO = { ...mockLobby, name: 'Updated Lobby' };
    mockedAxios.put.mockResolvedValue({ data: updatedLobby });
    const response = await lobbyService.updateLobby(updatedLobby);
    expect(mockedAxios.put).toHaveBeenCalledWith('/lobbies', updatedLobby);
    expect(response.data.name).toBe('Updated Lobby');
  });
});
