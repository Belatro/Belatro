import * as React from 'react';
import { NavigationContainer } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import './services/api';
import { AuthProvider } from './context/authContext';

import SignInScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\SignInScreen';
import SignUpScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\SignUpScreen';
import HomeScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\HomeScreen';
import LobbyListScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\LobbyListScreen';
import LobbyDetailsScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\LobbyDetailsScreen';
import MatchScreen from 'C:\\Users\\marko\\Desktop\\Projekti\\OICAR\\belatro-mobile\\Belatro\\pages\\MatchScreen';


export type RootStackParamList = {
  SignIn: undefined;
  SignUp: undefined;
  Home: {
    userId?: string;
    username?: string;
    email?: string;
    passwordHashed?: string;
    token?: string;
    openCreateLobby?: boolean;
  };
  LobbyList: undefined;
  LobbyDetails: { lobbyId: string, password?: string };
  Match: { matchId: string };
};

const Stack = createNativeStackNavigator<RootStackParamList>();

export default function App() {
  return (
    <AuthProvider>
      <NavigationContainer>
        <Stack.Navigator initialRouteName="SignIn">
          <Stack.Screen name="SignIn" component={SignInScreen} options={{ headerShown: false }} />
          <Stack.Screen name="SignUp" component={SignUpScreen} options={{ headerShown: false }} />
          <Stack.Screen name="Home" component={HomeScreen} options={{ headerShown: false }} />
          <Stack.Screen name="LobbyList" component={LobbyListScreen} />
          <Stack.Screen name="LobbyDetails" component={LobbyDetailsScreen} options={{ headerShown: false }} />
          <Stack.Screen name="Match" component={MatchScreen} options={{ headerShown: false }} />
        </Stack.Navigator>
      </NavigationContainer>
    </AuthProvider>
  );
}