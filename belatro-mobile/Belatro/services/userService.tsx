import axios from 'axios';
import AsyncStorage from '@react-native-async-storage/async-storage';

const API_URL = 'http://10.0.2.2:8080';

const axiosInstance = axios.create({
  baseURL: API_URL,
});

axiosInstance.interceptors.request.use(
  async config => {
    const token = await AsyncStorage.getItem('jwtToken');
    console.log('Token in interceptor:', token);
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  error => Promise.reject(error)
);

export interface RegisterPayload {
  username: string;
  email: string;
  password: string;
}

export interface UserUpdatePayload {
  username: string;
  email: string;
  passwordHashed: string;
}

export interface User {
  id: string;
  username: string;
  email: string;
}

export async function registerUser(payload: RegisterPayload) {
  return axiosInstance.post('/api/auth/signup', payload, { 
    headers: { 'Content-Type': 'application/json' },
  });
}

export async function getAllUsers() {
  return axiosInstance.get('/user/findAll');
}

export async function updateUser(id: string, payload: UserUpdatePayload) {
  return axiosInstance.put(`/user/${id}`, payload, {
    headers: { 'Content-Type': 'application/json' },
  });
}

export async function loginUser(loginRequest: { username: string; password: string }) {
  return axiosInstance.post('/api/auth/login', loginRequest);
}

export async function getUserById(id: string) {
  return axiosInstance.get(`/user/${id}`);
}
