import axiosInstance from './api';

export interface MatchDTO {
    id: string;
    lobbyId: string;
    players: { id: string; username: string }[];
    currentTurn: string;
    status: 'CREATED' | 'IN_PROGRESS' | 'COMPLETED';
    createdAt: Date;
}

export interface PrivateState {
    hand: Array<{ rank: string; suit: string }>;
    yourTurn: boolean;
    score: number;
}

export interface Card {
    rank: string;
    boja: string;
}

export interface PlayCardPayload {
    card: Card;
    declareBela: boolean;
}

export const matchService = {
    createMatch: (matchData: { lobbyId: string }) =>
        axiosInstance.post<MatchDTO>('/matches', matchData),

    getMatchById: (matchId: string) =>
        axiosInstance.get<MatchDTO>(`/matches/${matchId}`),

    getPrivateMatchState: (matchId: string, token: string) =>
        axiosInstance.get<PrivateState>(`/matches/${matchId}/private`, {
            headers: { Authorization: `Bearer ${token}` }
        }),

    playCard: (matchId: string, payload: PlayCardPayload, token: string) =>
        axiosInstance.post(`/matches/${matchId}/play`, payload, {
            headers: { Authorization: `Bearer ${token}` }
        }),

    makeBid: (matchId: string, payload: { pass: boolean; trump?: string }, token: string) =>
        axiosInstance.post(`/matches/${matchId}/bid`, payload, {
            headers: { Authorization: `Bearer ${token}` }
        }),

    refreshMatch: (matchId: string, token: string) =>
        axiosInstance.post(`/matches/${matchId}/refresh`, {}, {
            headers: { Authorization: `Bearer ${token}` }
        }),

    getMatchByLobbyId: (lobbyId: string) =>
        axiosInstance.get<MatchDTO>(`/matches/getmatchbylobbyid/${lobbyId}`)
};
