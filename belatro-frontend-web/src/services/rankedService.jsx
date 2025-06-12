import axios from 'axios';

const API_BASE_URL = process.env.REACT_APP_API_URL;

// POST join queue
export const joinRankedQueue = async (jwtToken) => {
  await axios.post(`${API_BASE_URL}/ranked/queue`, {}, {
    headers: {
      Authorization: `Bearer ${jwtToken}`,
    },
  });
};

// DELETE leave queue
export const leaveRankedQueue = async (jwtToken) => {
  await axios.delete(`${API_BASE_URL}/ranked/queue`, {
    headers: {
      Authorization: `Bearer ${jwtToken}`,
    },
  });
};
