import React from 'react';
import { View, Text, StyleSheet } from 'react-native';
import { BarCodeScanner } from 'expo-barcode-scanner';
import { api } from '../services/api';

export default function ScannerScreen() {
  const handleScan = ({ data }) => {
    api.post('/api/student/attendance/scan', { token: data }).catch(() => undefined);
  };

  return (
    <View style={styles.container}>
      <Text style={styles.title}>QR Scanner</Text>
      <View style={styles.scannerBox}>
        <BarCodeScanner onBarCodeScanned={handleScan} style={StyleSheet.absoluteFillObject} />
      </View>
    </View>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, padding: 20, backgroundColor: '#f5f7fb' },
  title: { fontSize: 22, fontWeight: '700', marginBottom: 12 },
  scannerBox: { height: 320, borderRadius: 12, overflow: 'hidden', backgroundColor: '#000' }
});
