import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { api } from '../services/api';

export default function DashboardScreen() {
  const [summary, setSummary] = useState(null);

  useEffect(() => {
    api.get('/api/campus/summary')
      .then((res) => setSummary(res.data))
      .catch(() => setSummary(null));
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Smart Campus Dashboard</Text>
      <Text style={styles.row}>Live tracked: {summary?.total ?? '--'}</Text>
      <Text style={styles.row}>Suspicious: {summary?.suspicious ?? '--'}</Text>
      <Text style={styles.row}>Verified: {summary?.verified ?? '--'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#f5f7fb' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 12 },
  row: { fontSize: 16, marginBottom: 8 }
});
