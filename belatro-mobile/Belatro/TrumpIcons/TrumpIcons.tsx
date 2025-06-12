import React from 'react';
import { Image, StyleSheet } from 'react-native';

const TrumpIcons = {
  KARA: require('../components/TrumpIcons/karaIcon.png'),
  HERC: require('../components/TrumpIcons/hercIcon.png'),
  TREF: require('../components/TrumpIcons/trefIcon.png'),
  PIK: require('../components/TrumpIcons/pikIcon.png'),
};

export const getTrumpIcon = (trump: string | undefined) => {
  if (!trump) return null;
  const icon = TrumpIcons[trump as keyof typeof TrumpIcons];
  return <Image source={icon} style={styles.icon} />;
};

const styles = StyleSheet.create({
  icon: {
    width: 40,
    height: 40,
    resizeMode: 'contain'
  }
});
