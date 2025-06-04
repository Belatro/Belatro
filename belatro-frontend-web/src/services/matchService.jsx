import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/matches';

// POST create match
export const createMatch = async (matchData) => {
  const response = await axios.post(`${API_BASE_URL}`, matchData);
  return response.data;
};

// GET match by ID - public 
export const getMatchById = async (matchId) => {
  const response = await axios.get(`${API_BASE_URL}/${matchId}`);
  return response.data;
};

// GET match state za trenutnog usera - private view
export const getPrivateMatchState = async (matchId, jwtToken) => {
  const response = await axios.get(`${API_BASE_URL}/${matchId}/private`, {
    headers: {
      Authorization: `Bearer ${jwtToken}`,
    },
  });
  return response.data;
};

// POST play card
export const playCard = async (matchId, payload, jwtToken) => {
  const response = await axios.post(
    `${API_BASE_URL}/${matchId}/play`,
    payload,
    {
      headers: {
        Authorization: `Bearer ${jwtToken}`,
      },
    }
  );
  return response.data;
};

// POST make bid
export const makeBid = async (matchId, payload, jwtToken) => {
  const response = await axios.post(
    `${API_BASE_URL}/${matchId}/bid`,
    payload,
    {
      headers: {
        Authorization: `Bearer ${jwtToken}`,
      },
    }
  );
  return response.data;
};

// POST refresh match state
export const refreshMatch = async (matchId, jwtToken) => {
  const response = await axios.post(
    `${API_BASE_URL}/${matchId}/refresh`,
    {},
    {
      headers: {
        Authorization: `Bearer ${jwtToken}`,
      },
    }
  );
  return response.data;
};
