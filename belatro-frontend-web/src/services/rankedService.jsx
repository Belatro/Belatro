import axios from 'axios';

const API_BASE_URL = 'http://localhost:8080/ranked';

// POST join queue
export const joinRankedQueue = async (jwtToken) => {
  await axios.post(`${API_BASE_URL}/queue`, {}, {
    headers: {
      Authorization: `Bearer ${jwtToken}`,
    },
  });
};

// DELETE leave queue
export const leaveRankedQueue = async (jwtToken) => {
  await axios.delete(`${API_BASE_URL}/queue`, {
    headers: {
      Authorization: `Bearer ${jwtToken}`,
    },
  });
};
