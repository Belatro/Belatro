import React, { useEffect, useState } from 'react';
import { View, Text, TouchableOpacity, StyleSheet } from 'react-native';
import { lobbyService } from '../services/lobbyService';
import { useAuth } from '../context/authContext';
import styles from '../styles/styles';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { LobbyDTO } from '../services/lobbyService';


type Props = NativeStackScreenProps<RootStackParamList, 'LobbyDetails'>;

export default function LobbyDetailsScreen({ route, navigation }: Props) {
  const { user } = useAuth();
  const { lobbyId } = route.params;
  const [lobby, setLobby] = useState<LobbyDTO | null>(null);

  useEffect(() => {
    fetchLobby();
    const interval = setInterval(fetchLobby, 1000);
    return () => clearInterval(interval);
  }, [lobbyId]);

  const fetchLobby = async () => {
    try {
      const response = await lobbyService.getLobbyDetails(lobbyId);
      setLobby(response.data);
    } catch (error) {
      console.error('Error fetching lobby:', error);
    }
  };

  const handleStartMatch = async () => {
    try {
      await lobbyService.startMatch(lobbyId);
    } catch (error) {
      console.error('Error starting match:', error);
    }
  };

  const handleJoinTeam = async (team: 'A' | 'B') => {
    if (!user || !lobby) return;
    try {
      await lobbyService.switchTeam({
        lobbyId: lobby.id,
        userId: user.id,
        targetTeam: team,
      });
    } catch (error) {
      console.error('Error switching team:', error);
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
      <View style={styles.lobbyDetailsBox}>
        <View style={styles.lobbyDetailsTeamsRow}>
          <View style={styles.lobbyDetailsTeamColumn}>{teamA}</View>
          <Text style={styles.lobbyDetailsVS}>VS</Text>
          <View style={styles.lobbyDetailsTeamColumn}>{teamB}</View>
        </View>
        <View style={styles.lobbyDetailsBottomRow}>
          <TouchableOpacity style={styles.lobbyDetailsLeaveButton} onPress={handleLeaveLobby}>
            <Text style={styles.lobbyDetailsLeaveButtonText}>
              {lobby.hostUser.id === user?.id ? 'Delete lobby' : 'Leave lobby'}
            </Text>
          </TouchableOpacity>
          <TouchableOpacity
            style={styles.lobbyDetailsStartButton}
            onPress={handleStartMatch}
            disabled={!user || lobby.hostUser.id !== user.id}
          >
            <Text style={styles.lobbyDetailsStartButtonText}>Play</Text>
          </TouchableOpacity>
          <View style={styles.lobbyDetailsCodeBox}>
            <Text style={styles.lobbyDetailsCodeText}>code: {lobby.id.slice(-4)}</Text>
          </View>
        </View>
      </View>
    </View>
  );
}

