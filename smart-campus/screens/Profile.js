import React, { useEffect } from 'react';
import { View, Text, StyleSheet } from 'react-native';
import * as Location from 'expo-location';

export default function ProfileScreen() {
  useEffect(() => {
    let timer;
    Location.requestForegroundPermissionsAsync().then((permission) => {
      if (permission.status !== 'granted') {
        return;
      }
      timer = setInterval(() => {
        Location.getCurrentPositionAsync({}).then(() => undefined).catch(() => undefined);
      }, 5000);
    });

    return () => {
      if (timer) {
        clearInterval(timer);
      }
    };
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Profile & Tracking</Text>
      <Text style={styles.body}>Location heartbeat runs every 5s when permission is granted.</Text>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#f5f7fb' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 12 },
  body: { fontSize: 16, lineHeight: 22 }
});
