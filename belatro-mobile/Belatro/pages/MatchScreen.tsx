import React, { useEffect, useRef, useState } from 'react';
import { View, Text, Image, TouchableOpacity, StyleSheet, Alert } from 'react-native';
import { Client } from '@stomp/stompjs';
import { useAuth } from '../context/authContext';
import { Card, PublicGameView, PrivateGameView, PlayerPublicInfo, MoveDTO, TrickPlay } from '../types/gameTypes';
import { CardImages } from '../CardImages/cards';
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';
import { getTrumpIcon } from '../TrumpIcons/TrumpIcons';
import styles from '../styles/styles';

const STACK_OFFSET = 0;

type MatchScreenProps = NativeStackScreenProps<RootStackParamList, 'Match'>;

const MatchScreen: React.FC<MatchScreenProps> = ({ route }) => {
    const { matchId } = route.params;
    const { user } = useAuth();
    const [publicState, setPublicState] = useState<PublicGameView | null>(null);
    const [privateState, setPrivateState] = useState<PrivateGameView | null>(null);
    const [teammate, setTeammate] = useState<PlayerPublicInfo | null>(null);
    const [opponentLeft, setOpponentLeft] = useState<PlayerPublicInfo | null>(null);
    const [opponentRight, setOpponentRight] = useState<PlayerPublicInfo | null>(null);
    const [selectedCard, setSelectedCard] = useState<Card | null>(null);
    const [connectionStatus, setConnectionStatus] = useState('connecting');
    const [visibleTrickPlays, setVisibleTrickPlays] = useState<TrickPlay[]>([]);
    const stompClient = useRef<Client | null>(null);
    const reconnectAttempts = useRef(0);
    const [, forceUpdate] = useState(0);
    useEffect(() => {
        if (!publicState || publicState.gameState === "BIDDING") {
            setVisibleTrickPlays([]);
            return;
        }
        if (publicState.currentTrick?.plays && publicState.seatingOrder) {
            const plays = publicState.seatingOrder
                .map(p => ({
                    playerId: p.id,
                    card: publicState.currentTrick.plays![p.id] || null
                }))
                .filter(p => p.card);
            setVisibleTrickPlays(plays);
        } else {
            setVisibleTrickPlays([]);
        }
    }, [publicState]);

    useEffect(() => {
        if (publicState?.gameState === "BIDDING") setVisibleTrickPlays([]);
    }, [publicState?.gameState]);

    const TrumpIcons = {
        HERC: require('../components/TrumpIcons/hercIcon.png'),
        KARA: require('../components/TrumpIcons/karaIcon.png'),
        TREF: require('../components/TrumpIcons/trefIcon.png'),
        PIK: require('../components/TrumpIcons/pikIcon.png'),
    };

    function parseCardString(cardString: string): Card {
        const suitMap: Record<string, string> = {
            '♠': 'PIK',
            '♥': 'HERC',
            '♦': 'KARA',
            '♣': 'TREF',
        };
        const rankMap: Record<string, string> = {
            'A': 'AS',
            'K': 'KRALJ',
            'Q': 'BABA',
            'J': 'DECKO',
            '7': 'SEDMICA',
            '8': 'OSMICA',
            '9': 'DEVETKA',
            '1': 'DESETKA',
        };

        let rankSymbol = cardString[0];
        let suitSymbol = cardString[cardString.length - 1];
        if (cardString.startsWith('10')) {
            rankSymbol = '1';
            suitSymbol = cardString[2];
        }

        const boja = suitMap[suitSymbol] || 'UNKNOWN';
        const rank = rankMap[rankSymbol] || 'UNKNOWN';

        return { boja, rank };
    }

    function getCardImage(card: Card) {
        const suitMap = { HERC: 'herc', KARA: 'kara', PIK: 'pik', TREF: 'tref' };
        const rankMap = {
            AS: 'As', BABA: 'Baba', DECKO: 'Decko', KRALJ: 'Kralj',
            SEDMICA: '7', OSMICA: '8', DEVETKA: '9', DESETKA: '10'
        };
        const suit = suitMap[card.boja as keyof typeof suitMap];
        const rank = rankMap[card.rank as keyof typeof rankMap];
        const key = suit && rank ? `${suit}${rank}` : 'cardBack';
        return CardImages[key as keyof typeof CardImages] || CardImages.cardBack;
    }

    const connectWebSocket = (): Client => {
        const client = new Client({
            brokerURL: `ws://10.0.2.2:8080/ws-native?user=${user?.username}`,
            reconnectDelay: 5000,
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            forceBinaryWSFrames: true,
            appendMissingNULLonIncoming: true,
            connectHeaders: {
                Authorization: `Bearer ${user?.token}`,
                "X-Player-Name": user?.username || '',
                "X-Match-ID": matchId,
            },
            onConnect: () => {
                console.log('Connected :D');
                setConnectionStatus('connected');
                reconnectAttempts.current = 0;
                client.subscribe(`/topic/games/${matchId}`, (message) => {
                    const state = JSON.parse(message.body) as PublicGameView;
                    setPublicState(JSON.parse(JSON.stringify(state)));
                    if (state.seatingOrder) {
                        const meIndex = state.seatingOrder.findIndex(p => p.id === user?.username);
                        if (meIndex >= 0) {
                            const getAt = (offset: number) => ({
                                ...state.seatingOrder[(meIndex + offset) % state.seatingOrder.length]
                            });
                            setTeammate(getAt(2));
                            setOpponentLeft(getAt(3));
                            setOpponentRight(getAt(1));
                        }
                    }
                });
                client.subscribe(`/user/queue/games/${matchId}`, (message) => {
                    const privateState = JSON.parse(message.body) as PrivateGameView;
                    setPrivateState(privateState);
                });
                client.publish({ destination: `/app/games/${matchId}/refresh`, body: JSON.stringify({}) });
            },
            // debug: (str) => console.log('[STOMP]', str),
            onStompError: (error) => {
                console.error('STOMP error:', error);
                setConnectionStatus('error');
                handleReconnect();
            },
            onWebSocketError: (error) => {
                console.error('WebSocket error:', error);
                setConnectionStatus('error');
                handleReconnect();
            },
        });

        client.onDisconnect = () => {
            console.log('WebSocket disconnected');
            setConnectionStatus('disconnected');
            handleReconnect();
        };

        client.activate();
        return client;
    };

    const handleReconnect = () => {
        const delay = Math.min(1000 * 2 ** reconnectAttempts.current, 30000);
        setTimeout(() => {
            reconnectAttempts.current += 1;
            stompClient.current = connectWebSocket();
        }, delay);
    };

    useEffect(() => {
        stompClient.current = connectWebSocket();
        const pingInterval = setInterval(() => {
            if (stompClient.current?.connected) {
                stompClient.current.publish({
                    destination: `/app/games/${matchId}/ping`,
                    body: JSON.stringify({})
                });
            }
        }, 9000);
        return () => {
            if (stompClient.current?.connected) {
                stompClient.current.deactivate();
            }
            clearInterval(pingInterval);
        };
    }, [matchId, user?.token]);



    const handlePlayCard = () => {
        if (!selectedCard || !stompClient.current?.connected) return;

        stompClient.current.publish({
            destination: `/app/games/${matchId}/play`,
            body: JSON.stringify({
                playerId: user?.username, 
                card: selectedCard,
                declareBela: false
            })
        });
    };


    const handleBid = (pass: boolean, trump?: string) => {
        if (!stompClient.current?.connected) {
            // console.log('handleBid: Not connected to STOMP server');
            return;
        }

        const bidData = {
            playerId: user?.username,
            pass,
            ...(trump && { trump })
        };
        // console.log('handleBid: Sending bid:', JSON.stringify(bidData));

        stompClient.current.publish({
            destination: `/app/games/${matchId}/bid`,
            headers: {
                Authorization: `Bearer ${user?.token}`,
                "Content-Type": "application/json"
            },
            body: JSON.stringify(bidData)
        });
    };

    const handleChallenge = () => {
        if (!stompClient.current?.connected) return;
        stompClient.current.publish({
            destination: `/app/games/${matchId}/challenge`,
            body: JSON.stringify({ playerId: user?.username }), 
        });
    };

    const getYourTeam = () => {
        if (!publicState || !user?.username) return null;
        const isInTeamA = publicState.teamA?.some(p => p.username === user.username);
        const isInTeamB = publicState.teamB?.some(p => p.username === user.username);
        if (isInTeamA) return "You are on Team A";
        if (isInTeamB) return "You are on Team B";
        return null;
    };

    const trumpChosen = !!publicState?.bids?.find(b => b.selectedTrump);

    return (

        <View style={styles.matchcontainer}>
            <View style={styles.turnIndicatorContainer}>
                <Text style={styles.turnIndicatorText}>
                    {privateState?.yourTurn ? 'Your turn...' : 'Waiting...'}
                </Text>
            </View>
            <View style={styles.trumpIndicator}>
                {publicState?.bids?.find(b => b.selectedTrump)?.selectedTrump &&
                    getTrumpIcon(publicState.bids.find(b => b.selectedTrump)?.selectedTrump ?? undefined)}
            </View>
            {publicState?.teamAScore !== undefined && publicState?.teamBScore !== undefined && (
                <View style={styles.scoreboard}>
                    <Text style={styles.teamLabel}>{getYourTeam()}</Text>
                    <View style={styles.scoreRow}>
                        <Text style={{ color: "limegreen" }}>Team A: {publicState.teamAScore}</Text>
                        <Text style={{ color: "red" }}>Team B: {publicState.teamBScore}</Text>
                    </View>
                </View>
            )}
            {connectionStatus === 'reconnecting' && (
                <View style={styles.reconnectingBanner}>
                    <Text style={styles.bannerText}>Reconnecting...</Text>
                </View>
            )}
            {connectionStatus === 'error' && (
                <View style={styles.errorBanner}>
                    <Text style={styles.errorText}>Connection lost. Please refresh.</Text>
                </View>
            )}
            <View style={styles.topPlayer}>
                <Image source={require('../components/Profile.png')} style={styles.avatar} />
                <Text style={styles.username}>{teammate?.username}</Text>
            </View>

            <View style={styles.leftPlayer}>
                <Image source={require('../components/Profile.png')} style={styles.avatar} />
                <Text style={styles.username}>{opponentLeft?.username}</Text>
            </View>

            <View style={styles.rightPlayer}>
                <Image source={require('../components/Profile.png')} style={styles.avatar} />
                <Text style={styles.username}>{opponentRight?.username}</Text>
            </View>

            <View style={styles.playArea}>
                {visibleTrickPlays.map((play, idx) => (
                    <Image
                        key={idx}
                        source={getCardImage(play.card)}
                        style={[styles.card, { marginLeft: idx > 0 ? 20 : -110 }]}
                    />
                ))}
            </View>
            <View style={styles.handRow}>
                <TouchableOpacity
                    style={styles.playCardButton}
                    onPress={handlePlayCard}
                    disabled={!privateState?.yourTurn}
                >
                    <Text style={styles.buttonText}>Play Card</Text>
                </TouchableOpacity>
                <View style={styles.handContainer}>
                    {privateState?.hand?.map((card, index) => (
                        <TouchableOpacity
                            key={index}
                            onPress={() => setSelectedCard(card)}
                            style={[
                                styles.cardContainer,
                                selectedCard?.boja === card.boja && selectedCard?.rank === card.rank
                                    ? styles.selectedCard
                                    : {}
                            ]}
                        >
                            <Image
                                source={getCardImage(card)}
                                style={[styles.handCard, { marginLeft: index > 0 ? -STACK_OFFSET : 0 }]}
                            />
                        </TouchableOpacity>
                    ))}
                </View>
                <TouchableOpacity
                    style={styles.challengeButton}
                    onPress={handleChallenge}
                    disabled={!privateState?.yourTurn}
                >
                    <Text style={styles.buttonTextAction}>Challenge</Text>
                </TouchableOpacity>
            </View>
            <View style={styles.actions}>
                {publicState?.gameState === 'BIDDING' &&
                    privateState?.yourTurn &&
                    !trumpChosen && (
                        <View style={styles.bidButtons}>
                            <TouchableOpacity style={styles.bidButton} onPress={() => handleBid(false, 'HERC')}>
                                <Image source={TrumpIcons.HERC} style={styles.trumpIcon} />
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.bidButton} onPress={() => handleBid(false, 'KARA')}>
                                <Image source={TrumpIcons.KARA} style={styles.trumpIcon} />
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.bidButton} onPress={() => handleBid(false, 'TREF')}>
                                <Image source={TrumpIcons.TREF} style={styles.trumpIcon} />
                            </TouchableOpacity>
                            <TouchableOpacity style={styles.bidButton} onPress={() => handleBid(false, 'PIK')}>
                                <Image source={TrumpIcons.PIK} style={styles.trumpIcon} />
                            </TouchableOpacity>
                            {publicState?.bids?.length < 3 && (
                                <TouchableOpacity style={styles.bidButton} onPress={() => handleBid(true)}>
                                    <Text style={styles.buttonTextAction}>Pass</Text>
                                </TouchableOpacity>
                            )}
                        </View>
                    )}
            </View>
        </View>
    );
};

export default MatchScreen;
