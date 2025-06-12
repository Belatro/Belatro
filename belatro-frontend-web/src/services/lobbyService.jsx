import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL;

// POST create lobby
export const createLobby = async (lobbyData) => {
  const response = await axios.post(`${API_BASE_URL}/lobbies`, lobbyData);
  return response.data;
};

// GET svi lobbyji
export const getAllLobbies = async () => {
  const response = await axios.get(`${API_BASE_URL}/lobbies`);
  return response.data;
};

// GET svi otvoreni lobbyji (moguci join/availible)
export const getAllOpenLobbies = async () => {
  const response = await axios.get(`${API_BASE_URL}/lobbies/open`);
  return response.data;
};

// POST join na postojeci lobby
export const joinLobby = async (joinRequest) => {
  const response = await axios.post(`${API_BASE_URL}/lobbies/join`, joinRequest);
  return response.data;
};

// GET lobby po id
export const getLobbyById = async (lobbyId) => {
  const response = await axios.get(`${API_BASE_URL}/lobbies/${lobbyId}`);
  return response.data;
};

// PUT update postojeci lobby (kad user joina npr)
export const updateLobby = async (lobbyData) => {
  const response = await axios.put(`${API_BASE_URL}/lobbies`, lobbyData);
  return response.data;
};

// POST svapaj tim useru
export const switchTeam = async (switchRequest) => {
  const response = await axios.post(`${API_BASE_URL}/lobbies/switchTeam`, switchRequest);
  return response.data;
};

// PATCH leave lobby
export const leaveLobby = async (lobbyId, leaveRequest) => {
  const response = await axios.patch(`${API_BASE_URL}/lobbies/${lobbyId}/leave`, leaveRequest);
  return response.data;
};

// DELETE lobby
export const deleteLobby = async (lobbyId) => {
  const response = await axios.delete(`${API_BASE_URL}/lobbies/${lobbyId}`);
  return response.data;
};

// POST start match
export const startMatch = async (lobbyId) => {
  const response = await axios.post(`${API_BASE_URL}/lobbies/${lobbyId}/start-match`);
  return response.data;
};