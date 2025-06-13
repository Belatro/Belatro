import React from 'react';
import { render, screen } from '@testing-library/react-native';
import { useAuth } from '../context/authContext';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { RouteProp } from '@react-navigation/native';
import HomeScreen from './HomeScreen'; 
import { RootStackParamList } from '../App';

jest.mock('../context/authContext', () => ({
    useAuth: jest.fn(),
}));

jest.mock('expo-screen-orientation', () => ({
    lockAsync: jest.fn(),
    unlockAsync: jest.fn(),
    OrientationLock: { LANDSCAPE_RIGHT: 'landscape-right' },
}));

const navigation = {
    navigate: jest.fn(),
    setOptions: jest.fn(),
} as unknown as NativeStackNavigationProp<RootStackParamList, 'Home'>;

const route = {
    key: 'home-screen-key',
    name: 'Home' as const,
    params: {},
} as RouteProp<RootStackParamList, 'Home'>;

describe('HomeScreen', () => {
    beforeEach(() => {
        (useAuth as jest.Mock).mockReturnValue({
            user: { id: 'user1', username: 'testuser' },
        });
    });

    it('renders the main title', () => {
        render(<HomeScreen navigation={navigation} route={route} />);
        expect(screen.getByText('Belatro')).toBeTruthy();
    });
});
