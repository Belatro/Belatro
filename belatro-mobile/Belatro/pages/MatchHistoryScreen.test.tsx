import React from 'react';
import { render, screen, waitFor } from '@testing-library/react-native';
import MatchHistoryScreen from './MatchHistoryScreen';
import { fetchMatchHistorySummary } from '../services/matchHistoryService';
import { RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { MatchHistorySummary } from '../services/matchHistoryService';

jest.mock('../services/matchHistoryService');

const mockedFetchMatchHistorySummary = fetchMatchHistorySummary as jest.MockedFunction<typeof fetchMatchHistorySummary>;

const navigation = {
    navigate: jest.fn(),
} as unknown as NativeStackNavigationProp<RootStackParamList, 'MatchHistory'>;

const route = {
    key: 'match-history-key',
    name: 'MatchHistory' as const,
    params: { userId: 'user1' },
} as RouteProp<RootStackParamList, 'MatchHistory'>;

const mockMatches: MatchHistorySummary[] = [
    {
        result: '10-5',
        yourOutcome: 'WIN', 
        gameMode: 'Classic',
        endTime: new Date('2025-06-10T12:00:00Z').toISOString(),
    },
    {
        result: '7-10',
        yourOutcome: 'LOSS', 
        gameMode: 'Quick',
        endTime: new Date('2025-06-11T15:30:00Z').toISOString(),
    },
];

describe('MatchHistoryScreen', () => {
    beforeEach(() => {
        jest.clearAllMocks();
    });

    it('renders match history with data', async () => {
        mockedFetchMatchHistorySummary.mockResolvedValue(mockMatches);

        render(<MatchHistoryScreen navigation={navigation} route={route} />);

        await waitFor(() => {
            expect(screen.getByText('MATCH HISTORY')).toBeTruthy();

            expect(screen.getByText('RESULT')).toBeTruthy();
            expect(screen.getByText('OUTCOME')).toBeTruthy();
            expect(screen.getByText('GAMEMODE')).toBeTruthy();
            expect(screen.getByText('DATE')).toBeTruthy();

            expect(screen.getByText('10-5')).toBeTruthy();
            expect(screen.getByText('WIN')).toBeTruthy();
            expect(screen.getByText('Classic')).toBeTruthy();
            expect(screen.getByText('10. 06. 2025.')).toBeTruthy();

            expect(screen.getByText('7-10')).toBeTruthy();
            expect(screen.getByText('LOSS')).toBeTruthy();
            expect(screen.getByText('Quick')).toBeTruthy();
            expect(screen.getByText('10. 06. 2025.')).toBeTruthy();
        });
    });

    it('renders "No matches found" when data is empty', async () => {
        mockedFetchMatchHistorySummary.mockResolvedValue([]);

        render(<MatchHistoryScreen navigation={navigation} route={route} />);

        await waitFor(() => {
            expect(screen.getByText('No matches found.')).toBeTruthy();
        });
    });
});
