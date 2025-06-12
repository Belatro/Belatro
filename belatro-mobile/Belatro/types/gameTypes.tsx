export interface Card {
    boja: string;
    rank: string;
}

export interface PlayerPublicInfo {
    id: string;
    username: string;
}

export enum Boja {
    HERC = "HERC",
    KARA = "KARA",
    PIK = "PIK",
    TREF = "TREF"
}

export interface BidDTO {
    playerId: string;
    action: string;
    selectedTrump: string | null;
}

export interface TrickDTO {
    trickNo: number;
    moves: MoveDTO[];
    plays?: { [playerId: string]: Card }; 
}

export interface TrickPlay {
    playerId: string; 
    card: Card;      
}

export interface MoveDTO {
    order: number;
    player: string; 
    card: string;   
}

export interface PublicGameView {
    gameId: string;
    gameState: string;
    bids: BidDTO[];
    currentTrick: TrickDTO;
    teamAScore: number;
    teamBScore: number;
    teamA: PlayerPublicInfo[];
    teamB: PlayerPublicInfo[];
    challengeUsedByPlayer: { [username: string]: boolean }; 
    winnerTeamId: string | null;
    tieBreaker: boolean;
    seatingOrder: PlayerPublicInfo[];
}

export interface PrivateGameView {
    hand: Card[];
    yourTurn: boolean;
    challengeUsed?: boolean;
}
