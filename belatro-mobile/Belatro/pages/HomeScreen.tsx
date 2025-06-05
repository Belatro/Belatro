import React, { useEffect, useState } from 'react';
import {
    Image,
    SafeAreaView,
    View,
    Text,
    TouchableOpacity,
    Modal,
    Pressable,
    ScrollView,
    TextInput,
    Alert,
} from 'react-native';
import * as ScreenOrientation from 'expo-screen-orientation';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { updateUser, getUserById } from '../services/userService';
import { friendshipService, UserSearchResult, Friendship } from '../services/friendshipService';
import styles from '../styles/styles';
import { lobbyService } from '../services/lobbyService';
import { useAuth } from '../context/authContext';


type HomeScreenProps = NativeStackScreenProps<RootStackParamList, 'Home'>;

export default function HomePage({ navigation, route }: HomeScreenProps) {
    const { user } = useAuth();
    const userId = user!.id;
    const [tutorialVisible, setTutorialVisible] = useState(false);
    const [gameModeModalVisible, setGameModeModalVisible] = useState(false);
    const [selectedMode, setSelectedMode] = useState<'friends' | 'competitive' | null>(null);
    const [language, setLanguage] = useState<'en' | 'hr'>('en');
    const [dropdownVisible, setDropdownVisible] = useState(false);
    const [profileModalVisible, setProfileModalVisible] = useState(false);
    const [friendsModalVisible, setFriendsModalVisible] = useState(false);
    const [changeModalVisible, setChangeModalVisible] = useState(false);
    const [changeField, setChangeField] = useState<'username' | 'email' | 'password' | null>(null);
    const [newValue, setNewValue] = useState('');
    const [activeFriendsTab, setActiveFriendsTab] = useState<'friends' | 'requests' | 'search'>('friends');
    const [searchQuery, setSearchQuery] = useState('');
    const [searchResults, setSearchResults] = useState<UserSearchResult[]>([]);
    const [isSearching, setIsSearching] = useState(false);
    const [isLoading, setIsLoading] = useState(false);
    const [friendRequests, setFriendRequests] = useState<Friendship[]>([]);
    const [isLoadingRequests, setIsLoadingRequests] = useState(false);
    const [friends, setFriends] = useState<Friendship[]>([]);
    const [isLoadingFriends, setIsLoadingFriends] = useState(false);
    const [inventoryModalVisible, setInventoryModalVisible] = useState(false);
    // const userId = route.params?.userId;
    const [username, setUsername] = useState(route.params?.username || 'Username');
    const [email, setEmail] = useState(route.params?.email || 'user@example.com');
    const [passwordHashed, setPasswordHashed] = useState(route.params?.passwordHashed || '********');
    const [createLobbyModalVisible, setCreateLobbyModalVisible] = useState(false);
    const [lobbyName, setLobbyName] = useState('');
    const [isPrivate, setIsPrivate] = useState(false);
    const [password, setPassword] = useState('');
    const handleSendRequest = async (toUserId: string) => {
        try {
            await friendshipService.sendFriendRequest(userId, toUserId);
            Alert.alert('Success', 'Friend request sent!');
        } catch (error) {
            Alert.alert('Error', 'Failed to send friend request');
        }
    };

    const handleSearch = async () => {
        setIsSearching(true);
        try {
            const users = await friendshipService.searchUsers(searchQuery);
            setSearchResults(users);
        } catch (e) {
            Alert.alert('Error', 'Failed to search players');
        } finally {
            setIsSearching(false);
        }
    };

    const fetchFriendRequests = async () => {
        setIsLoadingRequests(true);
        try {
            const all = await friendshipService.getFriendshipsByUser(userId);
            const incoming = all.filter(
                f => f.toUser.id === userId && f.status === 'PENDING'
            );
            setFriendRequests(incoming);
        } catch (e) {
            Alert.alert('Error', 'Failed to load friend requests');
        } finally {
            setIsLoadingRequests(false);
        }
    };

    useEffect(() => {
        if (activeFriendsTab === 'requests') {
            fetchFriendRequests();
        }
    }, [activeFriendsTab, friendsModalVisible]);

    const fetchFriends = async () => {
        setIsLoadingFriends(true);
        try {
            const all = await friendshipService.getFriendshipsByUser(userId);
            const accepted = all.filter(
                f => f.status === 'ACCEPTED' && (f.fromUser.id === userId || f.toUser.id === userId)
            );
            setFriends(accepted);
        } catch (e) {
            Alert.alert('Error', 'Failed to load friends');
        } finally {
            setIsLoadingFriends(false);
        }
    };

    useEffect(() => {
        if (activeFriendsTab === 'friends' && friendsModalVisible) {
            fetchFriends();
        }
    }, [activeFriendsTab, friendsModalVisible]);

    const handleUnfriend = async (friendshipId: string) => {
        try {
            await friendshipService.deleteFriendship(friendshipId);
            setFriends(prev => prev.filter(f => f.id !== friendshipId));
            Alert.alert('Unfriended');
        } catch (e) {
            Alert.alert('Error', 'Failed to unfriend');
        }
    };

    const handleCreateLobby = async () => {
    if (!user) return;
    try {
        const response = await lobbyService.createLobby({
            name: lobbyName,
            hostUser: { id: user.id, username: user.username },
            gameMode: 'FRIENDS',
            privateLobby: isPrivate,
            password: isPrivate ? password : undefined
        });
        setCreateLobbyModalVisible(false);
        navigation.navigate('LobbyDetails', { lobbyId: response.data.id });
    } catch (error) {
        console.error('Create lobby error:', (error as any)?.response?.data);
    }
};


    useEffect(() => {
        const lockOrientation = async () => {
            await ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.LANDSCAPE_RIGHT);
        };
        lockOrientation();
    }, []);

    const tutorialTexts = {
        en: {
            title: "How to Play Belot",
            body: `Belot is a popular card game played with 32 cards. The game is played by four players in two teams. The goal is to score points by winning tricks and declaring combinations. Each round, players bid for the right to choose the trump suit. The team that wins the bid tries to fulfill their contract, while the other team tries to prevent them. Points are scored for tricks and special card combinations. The first team to reach the target score wins!`
        },
        hr: {
            title: "Kako igrati Belot",
            body: `Belot je popularna kartaška igra koja se igra s 32 karte. Igru igraju četiri igrača u dva tima. Cilj je osvojiti bodove osvajanjem štihova i prijavljivanjem kombinacija. Svaku rundu igrači licitiraju za pravo izbora aduta. Tim koji pobijedi u licitaciji pokušava ispuniti svoj ugovor, dok ih protivnički tim pokušava spriječiti. Bodovi se osvajaju za štihove i posebne kombinacije karata. Prvi tim koji dosegne ciljanu količinu bodova pobjeđuje!`
        }
    };

    useEffect(() => {
        const fetchEmail = async () => {
            try {
                const response = await getUserById(userId);
                setEmail(response.data.email);
            } catch (error) {
                console.error("Failed to fetch email:", error);
            }
        };

        if (userId) {
            fetchEmail();
        }
    }, [userId]);

    return (
        <SafeAreaView style={{ flex: 1, backgroundColor: '#1e1e1e' }}>
            <Modal
                animationType="slide"
                transparent={true}
                visible={tutorialVisible}
                onRequestClose={() => setTutorialVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.modalContent}>
                        <View style={styles.languageRow}>
                            <View style={{ flex: 1 }} />
                            <TouchableOpacity
                                style={styles.languageButton}
                                onPress={() => setDropdownVisible(!dropdownVisible)}
                            >
                                <Text style={styles.languageButtonText}>Language</Text>
                            </TouchableOpacity>
                        </View>
                        {dropdownVisible && (
                            <View style={styles.dropdown}>
                                <TouchableOpacity
                                    style={styles.dropdownItem}
                                    onPress={() => {
                                        setLanguage('en');
                                        setDropdownVisible(false);
                                    }}
                                >
                                    <Text style={styles.dropdownText}>English</Text>
                                </TouchableOpacity>
                                <TouchableOpacity
                                    style={styles.dropdownItem}
                                    onPress={() => {
                                        setLanguage('hr');
                                        setDropdownVisible(false);
                                    }}
                                >
                                    <Text style={styles.dropdownText}>Croatian</Text>
                                </TouchableOpacity>
                            </View>
                        )}
                        <Text style={styles.modalTitle}>{tutorialTexts[language].title}</Text>
                        <Text style={styles.modalText}>{tutorialTexts[language].body}</Text>
                        <Pressable style={styles.closeButton} onPress={() => setTutorialVisible(false)}>
                            <Text style={styles.closeButtonText}>Close</Text>
                        </Pressable>
                    </View>
                </View>
            </Modal>

            <TouchableOpacity
                style={styles.profileContainer}
                onPress={() => setProfileModalVisible(true)}
            >
                <Image
                    source={require('../components/Profile.png')}
                    style={styles.image}
                />
                <Text style={styles.profileText}>{username}</Text>
            </TouchableOpacity>

            <Modal
                animationType="slide"
                transparent={true}
                visible={profileModalVisible}
                onRequestClose={() => setProfileModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.profileModalContent}>
                        <ScrollView contentContainerStyle={styles.profileScrollContainer}>
                            <Image
                                source={require('../components/Profile.png')}
                                style={styles.profileModalImage}
                            />
                            <View style={styles.profileRow}>
                                <Text style={styles.profileModalText}>{username}</Text>
                                <TouchableOpacity
                                    style={styles.changeButton}
                                    onPress={() => {
                                        setChangeField('username');
                                        setNewValue(username);
                                        setChangeModalVisible(true);
                                    }}
                                >
                                    <Text style={styles.changeButtonText}>Change username</Text>
                                </TouchableOpacity>
                            </View>
                            <View style={styles.profileRow}>
                                <Text style={styles.profileModalText}>{email}</Text>
                                <TouchableOpacity
                                    style={styles.changeButton}
                                    onPress={() => {
                                        setChangeField('email');
                                        setNewValue(email);
                                        setChangeModalVisible(true);
                                    }}
                                >
                                    <Text style={styles.changeButtonText}>Change email</Text>
                                </TouchableOpacity>
                            </View>
                            <View style={styles.profileButtonRow}>
                                <TouchableOpacity
                                    style={styles.profileModalButton}
                                    onPress={() => setFriendsModalVisible(true)}
                                >
                                    <Text style={styles.profileModalButtonText}>Friends</Text>
                                </TouchableOpacity>
                                <TouchableOpacity
                                    style={styles.profileModalButton}
                                    onPress={() => setInventoryModalVisible(true)}
                                >
                                    <Text style={styles.profileModalButtonText}>Inventory</Text>
                                </TouchableOpacity>
                            </View>
                        </ScrollView>
                        <View style={styles.profileModalButtons}>
                            <TouchableOpacity
                                style={styles.signOutButton}
                                onPress={async () => {
                                    await ScreenOrientation.unlockAsync();
                                    setProfileModalVisible(false);
                                    navigation.replace('SignIn');
                                }}
                            >
                                <Text style={styles.signOutButtonText}>Sign out</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.closeButton}
                                onPress={() => setProfileModalVisible(false)}
                            >
                                <Text style={styles.closeButtonText}>Close</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                animationType="slide"
                transparent={true}
                visible={friendsModalVisible}
                onRequestClose={() => setFriendsModalVisible(false)}
            >
                <View style={styles.friendsModalOverlay}>
                    <View style={styles.friendsModalContent}>
                        <View style={{ width: '100%', flexDirection: 'row', justifyContent: 'flex-end', }}>
                            <TouchableOpacity onPress={() => setFriendsModalVisible(false)}>
                                <Text style={styles.closeFriendsButton}>✕</Text>
                            </TouchableOpacity>
                        </View>
                        <View style={styles.friendsModalContent}>

                            <View style={styles.friendsModalTabs}>
                                <TouchableOpacity
                                    style={styles.friendsModalTab}
                                    onPress={() => setActiveFriendsTab('friends')}
                                >
                                    <Text style={styles.friendsModalTabText}>Friends</Text>
                                </TouchableOpacity>
                                <TouchableOpacity
                                    style={styles.friendsModalTab}
                                    onPress={() => setActiveFriendsTab('requests')}
                                >
                                    <Text style={styles.friendsModalTabText}>Friend Requests</Text>
                                </TouchableOpacity>
                                <TouchableOpacity
                                    style={styles.friendsModalTab}
                                    onPress={() => setActiveFriendsTab('search')}
                                >
                                    <Text style={styles.friendsModalTabText}>Search Players</Text>
                                </TouchableOpacity>
                            </View>
                            {activeFriendsTab === 'friends' && (
                                <ScrollView style={styles.searchResultsContainer}>
                                    {isLoadingFriends ? (
                                        <Text style={{ color: '#fff', textAlign: 'center' }}>Loading...</Text>
                                    ) : friends.length === 0 ? (
                                        <Text style={{ color: '#fff', textAlign: 'center' }}>No friends yet</Text>
                                    ) : (
                                        friends.map(friendship => {
                                            const friendUser =
                                                friendship.fromUser.id === userId
                                                    ? friendship.toUser
                                                    : friendship.fromUser;
                                            return (
                                                <View key={friendship.id} style={styles.searchResultItem}>
                                                    <Text style={styles.searchResultText}>{friendUser.username}</Text>
                                                    <TouchableOpacity
                                                        style={[styles.sendRequestButton, { backgroundColor: '#ff4d4d' }]}
                                                        onPress={() => handleUnfriend(friendship.id)}
                                                    >
                                                        <Text style={styles.sendRequestButtonText}>Unfriend</Text>
                                                    </TouchableOpacity>
                                                </View>
                                            );
                                        })
                                    )}
                                </ScrollView>
                            )}
                            {activeFriendsTab === 'requests' && (
                                <ScrollView style={styles.searchResultsContainer}>
                                    {isLoadingRequests ? (
                                        <Text style={{ color: '#fff', textAlign: 'center' }}>Loading...</Text>
                                    ) : friendRequests.length === 0 ? (
                                        <Text style={{ color: '#fff', textAlign: 'center' }}>No friend requests</Text>
                                    ) : (
                                        friendRequests.map(request => (
                                            <View key={request.id} style={styles.searchResultItem2}>
                                                <Text style={styles.searchResultText2}>
                                                    {request.fromUser.username}
                                                </Text>
                                                <TouchableOpacity
                                                    style={[styles.sendRequestButton, { backgroundColor: '#4CAF50' }]}
                                                    onPress={async () => {
                                                        try {
                                                            await friendshipService.acceptFriendRequest(request.id);
                                                            fetchFriendRequests();
                                                            Alert.alert('Friend request accepted!');
                                                        } catch (e) {
                                                            Alert.alert('Error', 'Failed to accept request');
                                                        }
                                                    }}
                                                >
                                                    <Text style={styles.sendRequestButtonText}>Accept</Text>
                                                </TouchableOpacity>
                                                <TouchableOpacity
                                                    style={[styles.sendRequestButton, { backgroundColor: '#ff4d4d', marginLeft: 20 }]}
                                                    onPress={async () => {
                                                        try {
                                                            await friendshipService.rejectFriendRequest(request.id);
                                                            fetchFriendRequests();
                                                            Alert.alert('Friend request rejected!');
                                                        } catch (e) {
                                                            Alert.alert('Error', 'Failed to reject request');
                                                        }
                                                    }}
                                                >
                                                    <Text style={styles.sendRequestButtonText}>Reject</Text>
                                                </TouchableOpacity>
                                            </View>
                                        ))
                                    )}
                                </ScrollView>
                            )}
                            {activeFriendsTab === 'search' && (
                                <View style={{ width: '100%' }}>
                                    <View style={styles.searchBarContainer}>
                                        <TextInput
                                            style={styles.searchInput}
                                            placeholder="Search by username..."
                                            placeholderTextColor="#888"
                                            value={searchQuery}
                                            onChangeText={setSearchQuery}
                                        />
                                        <TouchableOpacity
                                            style={styles.searchButton}
                                            onPress={handleSearch}
                                            disabled={isSearching}
                                        >
                                            <Text style={styles.searchButtonText}>
                                                {isSearching ? '...' : 'Search'}
                                            </Text>
                                        </TouchableOpacity>
                                    </View>
                                    <ScrollView style={styles.searchResultsContainer}>
                                        {searchResults.map(user => (
                                            <View key={user.id} style={styles.searchResultItem}>
                                                <Text style={styles.searchResultText}>{user.username ? String(user.username) : 'Unknown'}</Text>
                                                <TouchableOpacity
                                                    style={styles.sendRequestButton}
                                                    onPress={() => handleSendRequest(user.id)}
                                                >
                                                    <Text style={styles.sendRequestButtonText}>Send Request</Text>
                                                </TouchableOpacity>
                                            </View>
                                        ))}
                                    </ScrollView>
                                </View>
                            )}
                        </View>
                    </View>
                </View>
            </Modal>

            <Modal
                animationType="slide"
                transparent={true}
                visible={inventoryModalVisible}
                onRequestClose={() => setInventoryModalVisible(false)}
            >
                <View style={styles.inventoryModalOverlay}>
                    <View style={styles.inventoryModalContent}>
                        <Text style={styles.inventoryHeader}>SKIN INVENTORY</Text>
                        <Text style={styles.inventorySection}>CARD BACKS</Text>
                        <View style={styles.inventoryImageRow}>
                            <Image source={require('../components/CardBack1.png')} style={styles.inventoryImagePlaceholder} />
                            <Image source={require('../components/CardBack2.png')} style={styles.inventoryImagePlaceholder} />
                            <Image source={require('../components/CardBack3.png')} style={styles.inventoryImagePlaceholder} />
                        </View>
                        <Text style={styles.inventorySection}>CARD FRONTS</Text>
                        <View style={styles.inventoryImageRow}>
                            <Image source={require('../components/Herc Dečko.png')} style={styles.inventoryImagePlaceholder} />
                            <Image source={require('../components/Karo Kraljica.png')} style={styles.inventoryImagePlaceholder} />
                            <Image source={require('../components/Terf Dečko.png')} style={styles.inventoryImagePlaceholder} />
                        </View>
                        <TouchableOpacity
                            style={styles.inventoryCloseButton}
                            onPress={() => setInventoryModalVisible(false)}
                        >
                            <Text style={styles.inventoryCloseButtonText}>Close</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
            <Modal
                animationType="slide"
                transparent={true}
                visible={changeModalVisible}
                onRequestClose={() => setChangeModalVisible(false)}
            >
                <View style={styles.modalOverlay}>
                    <View style={styles.changeModalContent}>
                        <Text style={styles.changeModalTitle}>
                            Change {changeField === 'username' ? 'Username' : changeField === 'email' ? 'Email' : 'Password'}
                        </Text>
                        <TextInput
                            style={styles.changeModalInput}
                            value={newValue}
                            onChangeText={setNewValue}
                            autoCapitalize="none"
                            autoCorrect={false}
                            secureTextEntry={changeField === 'password'}
                            placeholder={`Enter new ${changeField}`}
                        />
                        <View style={{ flexDirection: 'row', marginTop: 20 }}>
                            <TouchableOpacity
                                style={styles.saveButton}
                                onPress={async () => {
                                    setIsLoading(true);
                                    try {
                                        if (changeField === 'username') {
                                            await updateUser(userId, {
                                                username: newValue,
                                                email: email,
                                                passwordHashed: passwordHashed,
                                            });
                                            setUsername(newValue);
                                        } else if (changeField === 'email') {
                                            await updateUser(userId, {
                                                username: username,
                                                email: newValue,
                                                passwordHashed: passwordHashed,
                                            });
                                            setEmail(newValue);
                                        }
                                        setChangeModalVisible(false);
                                    } catch (error) {
                                        Alert.alert('Error', 'Failed to update');
                                    } finally {
                                        setIsLoading(false);
                                    }
                                }}
                                disabled={isLoading}
                            >
                                <Text style={styles.saveButtonText}>{isLoading ? 'Saving...' : 'Save'}</Text>
                            </TouchableOpacity>
                            <TouchableOpacity
                                style={styles.closeButton}
                                onPress={() => setChangeModalVisible(false)}
                            >
                                <Text style={styles.closeButtonText}>Cancel</Text>
                            </TouchableOpacity>
                        </View>
                    </View>
                </View>
            </Modal>

            <View style={styles.headerContainer}>
                <Text style={styles.headerText}>Belatro</Text>
            </View>

            <View style={styles.buttonsContainer}>
                <TouchableOpacity
                    style={[styles.button, { width: 95 }]}
                    onPress={() => {
                        setSelectedMode('friends');
                        setGameModeModalVisible(true);
                    }}
                >
                    <Text style={styles.buttonText}>With Friends</Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.button, { width: 90 }]}
                    onPress={() => {
                        setSelectedMode('competitive');
                        setGameModeModalVisible(true);
                    }}
                >
                    <Text style={styles.buttonText}>Competitive</Text>
                </TouchableOpacity>
                <TouchableOpacity
                    style={[styles.button, { width: 85 }]}
                    onPress={() => setTutorialVisible(true)}
                >
                    <Text style={styles.buttonText}>Tutorial</Text>
                </TouchableOpacity>
            </View>
            <Modal
                visible={gameModeModalVisible}
                transparent
                animationType="fade"
                onRequestClose={() => setGameModeModalVisible(false)}
            >
                <View style={{
                    flex: 1,
                    backgroundColor: 'rgba(0,0,0,0.7)',
                    justifyContent: 'center',
                    alignItems: 'center'
                }}>
                    <View style={{
                        backgroundColor: '#2d112b',
                        borderRadius: 10,
                        padding: 20,
                        alignItems: 'center'
                    }}>
                        <TouchableOpacity
                            style={{
                                backgroundColor: '#8a2e2e',
                                borderRadius: 5,
                                marginBottom: 15,
                                width: 80,
                                alignItems: 'center',
                                padding: 10
                            }}
                            onPress={() => {
                                setGameModeModalVisible(false);
                                navigation.navigate('LobbyList');
                            }}
                        >
                            <Text style={{ color: '#fff', fontWeight: 'bold' }}>Join</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={{
                                backgroundColor: '#8a2e2e',
                                borderRadius: 5,
                                width: 80,
                                alignItems: 'center',
                                padding: 10
                            }}
                            onPress={() => {
                                setCreateLobbyModalVisible(true);
                                setGameModeModalVisible(false);
                            }}
                        >
                            <Text style={{ color: '#fff', fontWeight: 'bold' }}>Host</Text>
                        </TouchableOpacity>
                        <TouchableOpacity
                            style={{ marginTop: 15 }}
                            onPress={() => setGameModeModalVisible(false)}
                        >
                            <Text style={{ color: '#fff' }}>Close</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>
            <Modal visible={createLobbyModalVisible} transparent animationType="slide">
                <View style={styles.createLobbyModalContainer}>
                    <View style={styles.createLobbyModalContent}>
                        <Text style={styles.createLobbyModalTitle}>Create Lobby</Text>

                        <TextInput
                            style={styles.createLobbyModalInput}
                            placeholder="Lobby Name"
                            value={lobbyName}
                            onChangeText={setLobbyName}
                        />

                        <TouchableOpacity
                            style={[styles.createLobbyModalCheckbox, isPrivate && styles.createLobbyModalCheckboxChecked]}
                            onPress={() => setIsPrivate(!isPrivate)}
                        >
                            <Text style={styles.createLobbyModalCheckboxText}>Private Lobby</Text>
                        </TouchableOpacity>

                        {isPrivate && (
                            <TextInput
                                style={styles.createLobbyModalInput}
                                placeholder="Password"
                                secureTextEntry
                                value={password}
                                onChangeText={setPassword}
                            />
                        )}

                        <TouchableOpacity style={styles.createLobbyModalCreateButton} onPress={handleCreateLobby}>
                            <Text style={styles.buttonText}>Create</Text>
                        </TouchableOpacity>

                        <TouchableOpacity style={styles.createLobbyModalCancelButton} onPress={() => setCreateLobbyModalVisible(false)}>
                            <Text style={styles.createLobbyModalButtonText}>Cancel</Text>
                        </TouchableOpacity>
                    </View>
                </View>
            </Modal>

            <TouchableOpacity style={[styles.quitButton]}>
                <Text style={styles.quitButtonText}>Quit</Text>
            </TouchableOpacity>
        </SafeAreaView>
    );
}
