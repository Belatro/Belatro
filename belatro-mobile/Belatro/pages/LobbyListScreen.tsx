import React, { useEffect, useState, useCallback } from 'react';
import { View, Text, TouchableOpacity, StyleSheet, FlatList, Alert, ListRenderItem, Modal, TextInput } from 'react-native';
import { lobbyService } from '../services/lobbyService';
import { useAuth } from '../context/authContext';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { LobbyDTO } from '../services/lobbyService';
import styles from '../styles/styles';

type Props = NativeStackScreenProps<RootStackParamList, 'LobbyList'>;

export default function LobbyListScreen({ navigation }: Props) {
    const [lobbies, setLobbies] = useState<LobbyDTO[]>([]);
    const { user } = useAuth();
    const [passwordModalVisible, setPasswordModalVisible] = useState(false);
    const [passwordInput, setPasswordInput] = useState('');
    const [selectedLobbyId, setSelectedLobbyId] = useState<string | null>(null);

    useEffect(() => {
        fetchLobbies();
    }, []);

    const fetchLobbies = async () => {
        try {
            const response = await lobbyService.getAllOpenLobbies();
            setLobbies(response.data);
        } catch (error) {
            console.error('Error fetching lobbies:', error);
        }
    };

    const openPasswordModal = (lobbyId: string) => {
        setSelectedLobbyId(lobbyId);
        setPasswordInput('');
        setPasswordModalVisible(true);
    };

    const submitPassword = () => {
        if (selectedLobbyId) {
            handleJoinLobby(selectedLobbyId, passwordInput);
            setPasswordModalVisible(false);
        }
    };

    const handleJoinLobby = async (lobbyId: string, password?: string) => {
        if (!user) return;
        try {
            await lobbyService.joinLobby({
                lobbyId,
                userId: user.id,
                password
            });
            navigation.navigate('LobbyDetails', { lobbyId });
        } catch (error: any) {
            let message = 'Failed to join lobby. Please try again.';
            if (error.response?.status === 403) {
                message = 'Lobby is full or password is incorrect.';
            } else if (error.response?.status === 404) {
                message = 'Lobby not found.';
            } else if (error.response?.data?.message) {
                message = error.response.data.message;
            }
            Alert.alert('Error', message);
        }
    };



    const renderLobbyItem: ListRenderItem<LobbyDTO> = useCallback(
        ({ item }) => (
            <TouchableOpacity
                style={styles.lobbyItem}
                onPress={() => item.privateLobby
                    ? openPasswordModal(item.id)
                    : handleJoinLobby(item.id)}
            >
                <Text style={styles.lobbyName}>{item.name}</Text>
                <Text>Host: {item.hostUser?.username}</Text>
                <Text>
                    Players: {(item.teamAPlayers?.length || 0) +
                        (item.teamBPlayers?.length || 0) +
                        (item.unassignedPlayers?.length || 0)}/4
                </Text>
                {item.privateLobby && <Text>Private</Text>}
            </TouchableOpacity>
        ),
        [openPasswordModal, handleJoinLobby, styles]
    );

    return (
        <View style={styles.lobbycontainer}>
            <FlatList
                data={lobbies}
                keyExtractor={(item) => item.id}
                renderItem={renderLobbyItem}
            />
            <TouchableOpacity
                style={styles.createButtonlobby}
                onPress={() => navigation.navigate('Home', { openCreateLobby: true })}
            >
                <Text style={styles.buttonTextlobby}>Create Lobby</Text>
            </TouchableOpacity>
            <Modal visible={passwordModalVisible} transparent animationType="slide">
                <View style={{ flex: 1, justifyContent: 'center', alignItems: 'center', backgroundColor: 'rgba(0,0,0,0.5)' }}>
                    <View style={{ backgroundColor: '#222', padding: 20, borderRadius: 10 }}>
                        <Text style={{ color: '#fff', marginBottom: 10 }}>Enter Password:</Text>
                        <TextInput
                            style={{ backgroundColor: '#fff', borderRadius: 5, padding: 8, width: 200, marginBottom: 10 }}
                            secureTextEntry
                            value={passwordInput}
                            onChangeText={setPasswordInput}
                            placeholder="Password"
                        />
                        <TouchableOpacity onPress={submitPassword} style={{ marginBottom: 10 }}>
                            <Text style={{ color: '#4CAF50', fontWeight: 'bold' }}>Join</Text>
                        </TouchableOpacity>
                        <TouchableOpacity onPress={() => setPasswordModalVisible(false)}>
                            <Text style={{ color: '#f44336' }}>Cancel</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
        </View>
    );
}

