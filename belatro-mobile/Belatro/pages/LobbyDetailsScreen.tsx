import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, Alert } from 'react-native';
import { lobbyService } from '../services/lobbyService';
import { matchService } from '../services/matchService';
import { useAuth } from '../context/authContext';
import styles from '../styles/styles';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { LobbyDTO } from '../services/lobbyService';

type Props = NativeStackScreenProps<RootStackParamList, 'LobbyDetails'>;

export default function LobbyDetailsScreen({ route, navigation }: Props) {
  const { user } = useAuth();
  const { lobbyId, password } = route.params;
  const [lobby, setLobby] = useState<LobbyDTO | null>(null);
  const [matchStarted, setMatchStarted] = useState(false);

  const isInLobby = (lobby: LobbyDTO) => {
    return (
      lobby?.teamAPlayers?.some(p => p.id === user?.id) ||
      lobby?.teamBPlayers?.some(p => p.id === user?.id) ||
      lobby?.unassignedPlayers?.some(p => p.id === user?.id)
    );
  };

  const isHost = (lobby: LobbyDTO) => {
    return lobby?.hostUser?.id === user?.id;
  };

  const fetchLobby = async () => {
    try {
      const response = await lobbyService.getLobbyDetails(lobbyId);
      setLobby(response.data);

      if (user && !isInLobby(response.data) && !isHost(response.data)) {
        await lobbyService.joinLobby({
          lobbyId,
          userId: user.id,
          password: password || '',
        });
      }

      if (response.data.status === 'CLOSED' && !matchStarted) {
        try {
          const match = await matchService.getMatchByLobbyId(lobbyId);
          if (match.data?.id) {
            setMatchStarted(true);
            navigation.navigate('Match', { matchId: match.data.id });
          }
        } catch (err) {
          console.error('Failed to fetch match by lobby ID:', err);
        }
      }
    } catch (error) {
      console.error('Error fetching lobby:', error);
    }
  };

  useEffect(() => {
    fetchLobby();
    const interval = setInterval(fetchLobby, 1000);
    return () => clearInterval(interval);
  }, [lobbyId, user?.id]);

  const handleStartMatch = async () => {
    if (!lobby || !user) return;

    const isHost = lobby.hostUser?.id === user.id;
    if (!isHost) {
      Alert.alert('Error', 'Only the host can start the match.');
      return;
    }

    const totalPlayers = (lobby.teamAPlayers?.length || 0) + (lobby.teamBPlayers?.length || 0);
    if (totalPlayers !== 4) {
      Alert.alert('Error', 'Not enough players to start the game. There must be 4 players.');
      return;
    }

    try {
      const match = await lobbyService.startMatch(lobbyId);
      if (match.data?.id) {
        navigation.navigate('Match', { matchId: match.data.id });
      } else {
        console.warn('Match started, but no ID returned.');
      }
    } catch (error) {
      console.error('Error starting match:', error);
      Alert.alert('Error', 'Failed to start match.');
    }
  };

  const handleJoinTeam = async (team: 'A' | 'B') => {
    if (!user || !lobby) return;
    const isUnassigned = lobby.unassignedPlayers?.some(p => p.id === user.id);
    const currentTeam = lobby.teamAPlayers?.some(p => p.id === user.id) ? 'A' :
      lobby.teamBPlayers?.some(p => p.id === user.id) ? 'B' : null;

    try {
      if (isUnassigned) {
        await lobbyService.switchTeam({
          lobbyId: lobby.id,
          userId: user.id,
          targetTeam: team,
        });
      } else if (!currentTeam) {
        await lobbyService.joinLobby({
          lobbyId: lobby.id,
          userId: user.id,
          password: password || '',
        });
      } else if (currentTeam !== team) {
        await lobbyService.switchTeam({
          lobbyId: lobby.id,
          userId: user.id,
          targetTeam: team,
        });
      }
    } catch (error) {
      console.error('Error switching team:', error);
      Alert.alert('Error', 'Failed to join or switch team.');
    }
  };

  const handleLeaveLobby = async () => {
    if (!user || !lobby) return;
    try {
      if (lobby.hostUser.id === user.id) {
        await lobbyService.deleteLobby(lobby.id);
      } else {
        await lobbyService.leaveLobby(lobby.id, user.username);
      }
      navigation.goBack();
    } catch (error) {
      console.error('Error leaving or deleting lobby:', error);
    }
  };


  if (!lobby) return null;

  const renderPlayer = (player: { id: string; username: string }) => (
    <View style={styles.lobbyDetailsPlayerBox}>
      <Text style={styles.lobbyDetailsPlayerIcon}>👤</Text>
      <Text style={styles.lobbyDetailsPlayerName}>{player?.username || ''}</Text>
    </View>
  );

  const renderEmpty = (team: 'A' | 'B', index: number) => {
    const isOnThisTeam =
      (team === 'A' && lobby?.teamAPlayers?.some(p => p.id === user?.id)) ||
      (team === 'B' && lobby?.teamBPlayers?.some(p => p.id === user?.id));
    const canSwitch =
      user &&
      !isOnThisTeam &&
      (lobby?.unassignedPlayers?.some(p => p.id === user?.id) ||
        lobby?.teamAPlayers?.some(p => p.id === user?.id) ||
        lobby?.teamBPlayers?.some(p => p.id === user?.id));

    return (
      <View style={styles.lobbyDetailsPlayerBox} key={`empty-${team}-${index}`}>
        <Text style={styles.lobbyDetailsPlayerIcon}>👤</Text>
        {canSwitch ? (
          <TouchableOpacity onPress={() => handleJoinTeam(team)}>
            <Text style={[styles.lobbyDetailsPlayerName, { color: '#4CAF50', fontWeight: 'bold' }]}>
              Join
            </Text>
          </TouchableOpacity>
        ) : (
          <Text style={styles.lobbyDetailsPlayerName}>Waiting...</Text>
        )}
      </View>
    );
  };

  const teamA = [0, 1].map((index) => {
    const player = lobby.teamAPlayers[index];
    return player ? (
      <View key={player.id}>
        {renderPlayer(player)}
      </View>
    ) : (
      renderEmpty('A', index)
    );
  });

  const teamB = [0, 1].map((index) => {
    const player = lobby.teamBPlayers[index];
    return player ? (
      <View key={player.id}>
        {renderPlayer(player)}
      </View>
    ) : (
      renderEmpty('B', index)
    );
  });

  return (
    <View style={styles.lobbyDetailsContainer}>
      <View style={styles.lobbyDetailsIdBox}>
        <Text style={styles.lobbyDetailsIdText}>Lobby ID: {lobby.id}</Text>
      </View>
      <View style={styles.lobbyDetailsBox}>
        <View style={styles.lobbyDetailsTeamsRow}>
          <View style={styles.lobbyDetailsTeamColumn}>{teamA}</View>
          <Text style={styles.lobbyDetailsVS}>VS</Text>
          <View style={styles.lobbyDetailsTeamColumn}>{teamB}</View>
        </View>
        <View style={styles.lobbyDetailsBottomRow}>
          <TouchableOpacity style={styles.lobbyDetailsLeaveButton} onPress={handleLeaveLobby}>
            <Text style={styles.lobbyDetailsLeaveButtonText}>
              {isHost(lobby) ? 'Delete lobby' : 'Leave lobby'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.lobbyDetailsStartButton}
            onPress={handleStartMatch}
            disabled={!user || !isHost(lobby)}
          >
            <Text style={styles.lobbyDetailsStartButtonText}>Start</Text>
          </TouchableOpacity>
          <View style={styles.lobbyDetailsCodeBox}>
            <Text style={styles.lobbyDetailsCodeText}>
              {lobby.privateLobby && password ? `Password: ${password}` : lobby.privateLobby ? "Private Lobby" : "Public Lobby"}
            </Text>
          </View>
        </View>
      </View>
    </View>
  );
}
