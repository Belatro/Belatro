import React, { useEffect } from 'react';
import { View, Text, StyleSheet, Image, ImageSourcePropType } from 'react-native';
import * as ScreenOrientation from 'expo-screen-orientation';

const fakeName = 'Tvrtko';
const fakeHand: ImageSourcePropType[] = [
    require('../components/Herc 7.png'),
    require('../components/Herc 8.png'),
    require('../components/Herc 9.png'),
    require('../components/Herc 10.png'),
    require('../components/Herc Dečko.png'),
    require('../components/Karo 7.png'),
    require('../components/Karo Kraljica.png'),
    require('../components/Terf Dečko.png'),
];
const cardBack = require('../components/CardBack1.png');
const avatar = require('../components/Profile.png');

const CARD_WIDTH = 40;
const CARD_HEIGHT = 60;
const STACK_OFFSET = 14;

export default function GameScreen() {
    useEffect(() => {
        ScreenOrientation.lockAsync(ScreenOrientation.OrientationLock.LANDSCAPE_RIGHT);
        return () => {
            ScreenOrientation.unlockAsync();
        };
    }, []);

    const renderSideStack = () => (
        <View style={styles.sideStack}>
            {Array.from({ length: 8 }).map((_, i) => (
                <Image
                    key={i}
                    source={cardBack}
                    style={[
                        styles.sideCard,
                        { top: i * STACK_OFFSET, transform: [{ rotate: '90deg' }] }
                    ]}
                />
            ))}
        </View>
    );

    const renderHorizontalStack = (cards: ImageSourcePropType[], cardStyle: any) => (
        <View style={styles.horizontalStack}>
            {cards.map((img: ImageSourcePropType, i: number) => (
                <Image
                    key={i}
                    source={img}
                    style={[
                        cardStyle,
                        { left: i * STACK_OFFSET }
                    ]}
                />
            ))}
        </View>
    );

    const topCards = Array.from({ length: 8 }).map(() => cardBack);

    return (
        <View style={styles.root}>
            <View style={styles.sideColumn}>
                <View style={styles.profileColumn}>
                    <Image source={avatar} style={styles.avatarSmall} />
                    <Text style={styles.sideName}>{fakeName}</Text>
                </View>
                {renderSideStack()}
            </View>
            <View style={styles.centerArea}>
                <View style={styles.topArea}>
                    <Image source={avatar} style={styles.avatarSmall} />
                    <Text style={styles.username}>{fakeName}</Text>
                    <View style={styles.topStackContainer}>
                        {renderHorizontalStack(topCards, styles.topCard)}
                    </View>
                </View>
                <View style={styles.playSpace} />
                <View style={styles.bottomStackContainer}>
                    {renderHorizontalStack(fakeHand, styles.myCard)}
                </View>
            </View>
            <View style={styles.sideColumn}>
                {renderSideStack()}
                <View style={styles.profileColumn}>
                    <Image source={avatar} style={styles.avatarSmall} />
                    <Text style={styles.sideName}>{fakeName}</Text>
                </View>
            </View>
        </View>
    );
}

const styles = StyleSheet.create({
    root: {
        flex: 1,
        flexDirection: 'row',
        backgroundColor: '#07072a',
        alignItems: 'stretch',
        justifyContent: 'space-between',
    },
    sideColumn: {
        flexDirection: 'row',
        alignItems: 'center',
        justifyContent: 'center',
        marginHorizontal: 8,
    },
    profileColumn: {
        alignItems: 'center',
        justifyContent: 'center',
        marginHorizontal: 4,
    },
    avatarSmall: {
        width: 32,
        height: 32,
        borderRadius: 16,
        backgroundColor: '#fff',
        marginBottom: 2,
    },
    sideName: {
        color: '#fff',
        fontWeight: 'bold',
        fontSize: 13,
        marginTop: 2,
        marginBottom: 6,
        marginRight: 50,
        marginLeft: 50,
    },
    sideStack: {
        width: CARD_HEIGHT + 8,
        height: STACK_OFFSET * 7 + CARD_WIDTH,
        position: 'relative',
        alignItems: 'center',
        marginVertical: 4,
        marginLeft: 5,
        marginRight: 5,
    },
    sideCard: {
        position: 'absolute',
        width: CARD_HEIGHT + 10,
        height: CARD_WIDTH + 10,
        resizeMode: 'contain',
        left: 0,
    },
    centerArea: {
        flex: 1,
        justifyContent: 'space-between',
        alignItems: 'center',
    },
    topArea: {
        alignItems: 'center',
        marginTop: 8,
        marginBottom: 8,
    },
    username: {
        color: '#fff',
        fontWeight: 'bold',
        marginBottom: 4,
        fontSize: 13,
    },
    topStackContainer: {
        width: STACK_OFFSET * 7 + CARD_WIDTH,
        height: CARD_HEIGHT + 8,
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
        marginTop: 2,
        marginRight: 130,
    },
    horizontalStack: {
        position: 'relative',
        flexDirection: 'row',
        alignItems: 'center',
        height: CARD_HEIGHT,
        marginTop: 2,
    },
    topCard: {
        position: 'absolute',
        width: CARD_WIDTH,
        height: CARD_HEIGHT,
        resizeMode: 'contain',
        top: 0,
    },
    playSpace: {
        flex: 1,
        minHeight: 60,
        minWidth: 100,
    },
    bottomStackContainer: {
        width: STACK_OFFSET * (8 - 1) + CARD_WIDTH,
        height: CARD_HEIGHT + 8,
        justifyContent: 'center',
        alignItems: 'center',
        position: 'relative',
        marginBottom: 45,
        marginRight: 150,
    },
    myCard: {
        position: 'absolute',
        width: CARD_WIDTH + 30,
        height: CARD_HEIGHT + 30,
        resizeMode: 'contain',
        top: 0,
    },
});
