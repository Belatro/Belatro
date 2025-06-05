import axios from 'axios';
import { User, getAllUsers } from './userService';
import axiosInstance from './api';

const API_URL = 'http://10.0.2.2:8080/friendship';

export interface UserSearchResult {
    id: string;
    username: string;
    email: string;
}

export interface Friendship {
    id: string;
    fromUser: { id: string; username: string };
    toUser: { id: string; username: string };
    status: string;
}

export const friendshipService = {
    searchUsers: async (query: string): Promise<User[]> => {
        try {
            const response = await axiosInstance.get<User[]>('/user/findAll');
            return response.data.filter((user: User) =>
                user.username.toLowerCase().includes(query.toLowerCase())
            );
        } catch (error) {
            throw new Error('Failed to search users');
        }
    },

    sendFriendRequest: async (fromUserId: string, toUserId: string) => {
        return axiosInstance.post(`${API_URL}`, {
            fromUserId,
            toUserId
        });
    },

    getFriendshipsByUser: async (userId: string): Promise<Friendship[]> => {
        const res = await axiosInstance.get(`${API_URL}/getAllByUserId/${userId}`);
        return res.data;
    },

    acceptFriendRequest: async (friendshipId: string) => {
        return axiosInstance.post(`${API_URL}/${friendshipId}/accept`);
    },

    rejectFriendRequest: async (friendshipId: string) => {
        return axiosInstance.post(`${API_URL}/${friendshipId}/reject`);
    },

    deleteFriendship: async (friendshipId: string) => {
        return axiosInstance.delete(`${API_URL}/${friendshipId}`);
    },

};
