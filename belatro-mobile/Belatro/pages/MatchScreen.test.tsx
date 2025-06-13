import React from 'react';
import { render, screen } from '@testing-library/react-native';
import { useAuth } from '../context/authContext';
import MatchScreen from './MatchScreen';
import { RouteProp } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';

jest.mock('../context/authContext', () => ({
    useAuth: jest.fn(),
}));

jest.mock('@stomp/stompjs', () => ({
    Client: jest.fn().mockImplementation(() => ({
        brokerURL: '',
        connectHeaders: {},
        onConnect: jest.fn(),
        onStompError: jest.fn(),
        onWebSocketError: jest.fn(),
        onDisconnect: jest.fn(),
        subscribe: jest.fn(),
        publish: jest.fn(),
        activate: jest.fn(),
        deactivate: jest.fn(),
    })),
}));

jest.mock('../components/Profile.png', () => ({
    uri: 'mocked-profile-image',
}));
jest.mock('../components/TrumpIcons/hercIcon.png', () => ({
    uri: 'mocked-herc-icon',
}));
jest.mock('../components/TrumpIcons/karaIcon.png', () => ({
    uri: 'mocked-kara-icon',
}));
jest.mock('../components/TrumpIcons/trefIcon.png', () => ({
    uri: 'mocked-tref-icon',
}));
jest.mock('../components/TrumpIcons/pikIcon.png', () => ({
    uri: 'mocked-pik-icon',
}));

jest.mock('../CardImages/cards', () => ({
    cardBack: { uri: 'mocked-card-back' },
}));

const navigation = {
    navigate: jest.fn(),
    dispatch: jest.fn(),
} as unknown as NativeStackNavigationProp<RootStackParamList, 'Match'>;

const route = {
    key: 'match-screen-key',
    name: 'Match' as const,
    params: { matchId: 'match1' }, 
} as RouteProp<RootStackParamList, 'Match'>;

describe('MatchScreen', () => {
    beforeEach(() => {
        (useAuth as jest.Mock).mockReturnValue({
            user: { id: 'user1', username: 'testuser', token: 'testtoken' },
        });
    });

    afterEach(() => {
        const mockClient = require('@stomp/stompjs').Client.mock.instances[0];
        if (mockClient.deactivate) {
            mockClient.deactivate();
        }
        jest.clearAllMocks();
    });

    it('renders correctly with initial state', async () => {
        render(<MatchScreen navigation={navigation} route={route} />);
        expect(screen.getByText('Waiting...')).toBeTruthy();
    });
});
