import axios from 'axios';
import axiosInstance from './api';

const API_URL = 'http://10.0.2.2:8080/api/auth';
const USER_URL = 'http://10.0.2.2:8080/user';

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
  return axios.post(`${API_URL}/signup`, payload, { 
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
  return axios.post(`${API_URL}/login`, loginRequest);
}

export async function getUserById(id: string) {
  return axiosInstance.get(`${USER_URL}/${id}`);
}
