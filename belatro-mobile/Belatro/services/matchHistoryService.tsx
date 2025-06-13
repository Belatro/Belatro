import axiosInstance from './api'; 

export interface MatchHistorySummary {
  result: string;
  yourOutcome: 'WIN' | 'LOSS' | 'DRAW';
  gameMode: string;
  endTime: string;
 
}


export const fetchMatchHistorySummary = async (
  userId: string,
  page = 0,
  size = 20
): Promise<MatchHistorySummary[]> => {
  try {
    const response = await axiosInstance.get<{
      content: MatchHistorySummary[];
    }>(`/user/${userId}/history/summary?page=${page}&size=${size}`);

    console.log('Match history response:', response.data);
    return response.data.content;
  } catch (error) {
    console.error('Failed to fetch match history:', error);
    return [];
  }
};
