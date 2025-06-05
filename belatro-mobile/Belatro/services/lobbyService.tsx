import axiosInstance from './api';

export interface LobbyDTO {
    id: string;
    name: string;
    hostUser: { id: string; username: string };
    teamAPlayers: { id: string; username: string }[];
    teamBPlayers: { id: string; username: string }[];
    unassignedPlayers?: { id: string; username: string }[];
    gameMode?: string;
    status?: string;
    createdAt?: Date;
    privateLobby?: boolean;
    password?: string;
}

export interface CreateLobbyDTO {
    name: string;
    hostUser: { id: string; username: string };
    gameMode: string;
    privateLobby: boolean;
    password?: string;
}

export interface JoinLobbyRequestDTO {
    lobbyId: string;
    userId: string;
    password?: string;
}

export interface TeamSwitchRequestDTO {
    lobbyId: string;
    userId: string;
    targetTeam: 'A' | 'B';
}

export interface LeaveLobbyRequestDTO {
    username: string;
}

export const lobbyService = {
    getAllOpenLobbies: () => axiosInstance.get('/lobbies/open'),
    createLobby: (lobby: CreateLobbyDTO) => axiosInstance.post<LobbyDTO>('/lobbies', lobby),
    joinLobby: (request: JoinLobbyRequestDTO) => axiosInstance.post('/lobbies/join', request),
    getLobbyDetails: (lobbyId: string) => axiosInstance.get(`/lobbies/${lobbyId}`),
    startMatch: (lobbyId: string) => axiosInstance.post(`/lobbies/${lobbyId}/start-match`),
    switchTeam: (data: TeamSwitchRequestDTO) =>
        axiosInstance.post('/lobbies/switchTeam', data),
    leaveLobby: (lobbyId: string, username: string) =>
        axiosInstance.patch(`/lobbies/${lobbyId}/leave`, { username }),
    deleteLobby: (lobbyId: string) =>
        axiosInstance.delete(`/lobbies/${lobbyId}`),
};
