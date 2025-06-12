import React, { createContext, useContext, useState, useEffect } from 'react';
import AsyncStorage from '@react-native-async-storage/async-storage';
import { registerUser } from '../services/userService';
import { loginUser } from '../services/userService';

interface User {
  id: string;
  username: string;
  email?: string;
  token: string;
}

interface AuthContextType {
  user: User | null;
  token: string | null;
  login: (user: User, token: string) => Promise<void>;
  logout: () => Promise<void>;
  register: (userData: { username: string; email: string; password: string }) => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(null);
  const [token, setToken] = useState<string | null>(null);

  const register = async (userData: { username: string; email: string; password: string }) => {
  try {
    const response = await registerUser(userData);
    const loginResponse = await loginUser({
      username: userData.username,
      password: userData.password,
    });
    await login(loginResponse.data.user, loginResponse.data.token);
  } catch (error) {
    throw error;
  }
};

  useEffect(() => {
    const loadAuth = async () => {
      const savedToken = await AsyncStorage.getItem('jwtToken');
      const savedUser = await AsyncStorage.getItem('user');
      if (savedToken && savedUser) {
        setToken(savedToken);
        setUser(JSON.parse(savedUser));
      }
    };
    loadAuth();
  }, []);

  const login = async (user: User, token: string) => {
    console.log('Token after login:', token);
    setUser(user);
    setToken(token);
    await AsyncStorage.setItem('jwtToken', token);
    await AsyncStorage.setItem('user', JSON.stringify(user));
  };

  const logout = async () => {
    setUser(null);
    setToken(null);
    await AsyncStorage.removeItem('jwtToken');
    await AsyncStorage.removeItem('user');
  };

  return (
    <AuthContext.Provider value={{ user, token, login, logout, register }}>
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (context === undefined) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
