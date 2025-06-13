import React, { useEffect, useState } from 'react';
import { View, Text, FlatList, StyleSheet } from 'react-native';
import { fetchMatchHistorySummary, MatchHistorySummary } from '../services/matchHistoryService'; // Adjust path
import { NativeStackScreenProps } from '@react-navigation/native-stack';
import { RootStackParamList } from '../App';


type MatchHistoryScreenProps = NativeStackScreenProps<RootStackParamList, 'MatchHistory'>;

const MatchHistoryScreen: React.FC<MatchHistoryScreenProps> = ({ route, navigation }) => {
  const { userId } = route.params;

  const [matches, setMatches] = useState<MatchHistorySummary[]>([]);

  useEffect(() => {
    if (!userId) return;

    const loadHistory = async () => {
      try {
        const data = await fetchMatchHistorySummary(userId);
        setMatches(data);
      } catch (error) {
        console.error('Failed to load match history:', error);
        setMatches([]);
      }
    };

    loadHistory();
  }, [userId]);

  const renderItem = ({ item }: { item: MatchHistorySummary }) => (
    <View style={styles.row}>
      <Text style={styles.cell}>{item.result}</Text>
      <Text style={[styles.cell, item.yourOutcome === 'WIN' ? styles.positive : styles.negative]}>
        {item.yourOutcome}
      </Text>
      <Text style={styles.cell}>{item.gameMode}</Text>
      <Text style={styles.cell}>{new Date(item.endTime).toLocaleDateString()}</Text>
    </View>
  );

  return (
    <View style={styles.container}>
      <Text style={styles.title}>MATCH HISTORY</Text>
      <View style={styles.headerRow}>
        <Text style={styles.headerCell}>RESULT</Text>
        <Text style={styles.headerCell}>OUTCOME</Text>
        <Text style={styles.headerCell}>GAMEMODE</Text>
        <Text style={styles.headerCell}>DATE</Text>
      </View>
      <FlatList
        data={matches}
        renderItem={renderItem}
        keyExtractor={(item, index) => index.toString()}
        ListEmptyComponent={<Text style={styles.emptyText}>No matches found.</Text>}
      />
    </View>
  );
};

const styles = StyleSheet.create({
  container: { flex: 1, padding: 16, backgroundColor: '#1e1e1e' },
  title: { fontSize: 24, fontWeight: 'bold', marginBottom: 16, color: '#fff' },
  headerRow: { flexDirection: 'row', borderBottomWidth: 1, borderBottomColor: '#555' },
  headerCell: { flex: 1, fontWeight: 'bold', color: '#ccc' },
  row: { flexDirection: 'row', paddingVertical: 8, borderBottomWidth: 1, borderBottomColor: '#333' },
  cell: { flex: 1, color: '#eee' },
  positive: { color: 'limegreen' },
  negative: { color: 'red' },
  emptyText: { textAlign: 'center', marginTop: 20, color: '#888' },
});

export default MatchHistoryScreen;
