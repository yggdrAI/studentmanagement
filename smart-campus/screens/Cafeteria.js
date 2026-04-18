import React, { useEffect, useState } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { api } from '../services/api';

export default function CafeteriaScreen() {
  const [ai, setAi] = useState(null);

  useEffect(() => {
    api.get('/api/student/diet/suggestion')
      .then((res) => setAi(res.data))
      .catch(() => setAi(null));
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Diet AI</Text>
      <Text style={styles.item}>Prediction: {ai?.mlPrediction ?? '--'}</Text>
      <Text style={styles.item}>Score: {ai?.mlScore ?? '--'}</Text>
      <Text style={styles.item}>Recommendation: {ai?.recommendation ?? '--'}</Text>
      <Text style={styles.item}>Future risk: {ai?.futureRisk ?? '--'}</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#f5f7fb' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 12 },
  item: { fontSize: 16, marginBottom: 8 }
});
