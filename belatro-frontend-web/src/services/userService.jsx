import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL;

axios.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);


// GET za sve usere
export const fetchAllUsers = async () => {
  const response = await axios.get(`${API_BASE_URL}/user/findAll`);
  return response.data;
};

// GET za jednog usera po id
export const fetchUserById = async (id) => {
  const response = await axios.get(`${API_BASE_URL}/user/${id}`);
  return response.data;
};

// POST za register usera
export const registerUser = async (userData) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/signup`, {
      username: userData.username,
      email: userData.email,
      password: userData.passwordHashed,
    });
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// POST za login usera
export const loginUser = async ({ username, password }) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/api/auth/login`, {
      username,
      password,
    });
    return response.data;
  } catch (error) {
    throw error.response?.data?.message || "Login failed.";
  }
};

// PUT za editanje usera
export const updateUser = async (id, updatedUserData) => {
  try {
    const response = await axios.put(`${API_BASE_URL}/user/${id}`, updatedUserData);
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

//GET za user projateljstva po id
export const fetchAllFriendsByUserId = async (userId) => {
  const response = await axios.get(`${API_BASE_URL}/friendship/getAllByUserId/${userId}`);
  return response.data;
};

// GET za sve prijatelje za korisnika, sa filtriranjem na "PENDING" status
export const fetchFriendRequests = async (userId) => {
  const response = await axios.get(`${API_BASE_URL}/friendship/getAllByUserId/${userId}`);
  const pendingRequests = response.data.filter(friendship => friendship.status === "PENDING");
  return pendingRequests;
};

// POST posalji friend request
export const sendFriendRequest = async (fromUserId, toUserId) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/friendship`, {
      fromUserId,
      toUserId,
      status: "PENDING",
    });
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// POST cancelaj friend request
export const cancelFriendRequest = async (friendshipId) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/friendship/${friendshipId}/cancel`);
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// POST acceptaj friend request
export const acceptFriendRequest = async (friendshipId) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/friendship/${friendshipId}/accept`);
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// POST rejectaj firend
export const rejectFriendRequest = async (friendshipId) => {
  try {
    const response = await axios.post(`${API_BASE_URL}/friendship/${friendshipId}/reject`);
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// DELETE friendship po id
export const deleteFriendship = async (friendshipId) => {
  try {
    const response = await axios.delete(`${API_BASE_URL}/friendship/${friendshipId}`);
    return response.data;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};

// GET match history za usera po idu
export const fetchMatchHistorySummary = async (userId, page = 0, size = 20) => {
  try {
    const response = await axios.get(
      `${API_BASE_URL}/user/${userId}/history/summary?page=${page}&size=${size}`
    );
    console.log("Match history response:", response.data);
    return response.data.content;
  } catch (error) {
    throw error.response?.data || error.message;
  }
};