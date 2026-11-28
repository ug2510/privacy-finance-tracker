import React, { useState, useEffect } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TextInput,
  TouchableOpacity,
  Platform,
} from 'react-native';
import {
  getApiBaseUrl,
  setApiBaseUrl,
  resetApiBaseUrl,
} from '../../src/api/config';
import { checkBackendHealth } from '../../src/api/financeApi';

export default function SettingsScreen() {
  const [url, setUrl] = useState('');
  const [status, setStatus] = useState('');
  const [isHealthy, setIsHealthy] = useState(null);

  useEffect(() => {
    getApiBaseUrl().then(setUrl);
  }, []);

  const handleSaveAndTest = async () => {
    try {
      setStatus('Saving and testing connection...');
      await setApiBaseUrl(url);
      const ok = await checkBackendHealth();
      setIsHealthy(ok);
      setStatus(
        ok
          ? 'Connected! Spring Boot backend is UP.'
          : 'Saved, but backend health check did not return UP.'
      );
    } catch (err) {
      setIsHealthy(false);
      setStatus(`Connection failed: ${err.message}`);
    }
  };

  const handleReset = async () => {
    const def = await resetApiBaseUrl();
    setUrl(def);
    setIsHealthy(null);
    setStatus(`Reset to ${Platform.OS} default: ${def}`);
  };

  return (
    <ScrollView style={styles.container} contentContainerStyle={styles.content}>
      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Backend Server Connection</Text>
        <Text style={styles.helperText}>
          Running on {Platform.OS.toUpperCase()}. Web uses localhost:8080;
          Android uses your laptop&apos;s Wi-Fi IPv4 address.
        </Text>

        <TextInput
          style={styles.input}
          value={url}
          onChangeText={setUrl}
          placeholder="http://192.168.1.109:8080/api/v1"
          placeholderTextColor="#64748B"
          autoCapitalize="none"
          autoCorrect={false}
        />

        <View style={styles.row}>
          <TouchableOpacity style={styles.saveBtn} onPress={handleSaveAndTest}>
            <Text style={styles.saveBtnText}>Save & Test Connection</Text>
          </TouchableOpacity>
          <TouchableOpacity style={styles.resetBtn} onPress={handleReset}>
            <Text style={styles.resetBtnText}>Reset</Text>
          </TouchableOpacity>
        </View>

        {status ? (
          <Text
            style={[
              styles.statusText,
              {
                color:
                  isHealthy === true
                    ? '#34D399'
                    : isHealthy === false
                    ? '#F87171'
                    : '#94A3B8',
              },
            ]}
          >
            {status}
          </Text>
        ) : null}
      </View>

      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Android Bank SMS Sync</Text>
        <Text style={styles.helperText}>
          {Platform.OS === 'android'
            ? 'Native SMS permission & Indian bank alert parser will attach here in the APK build.'
            : 'SMS reading is only available on the Android APK. Use the Expenses tab to add transactions manually on Web.'}
        </Text>
      </View>
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#020617' },
  content: { padding: 16, maxWidth: 800, width: '100%', alignSelf: 'center' },
  card: {
    backgroundColor: '#0F172A',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#1E293B',
    marginBottom: 16,
  },
  sectionTitle: {
    color: '#F8FAFC',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 6,
  },
  helperText: { color: '#94A3B8', fontSize: 13, marginBottom: 12 },
  input: {
    backgroundColor: '#1E293B',
    color: '#F8FAFC',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
    fontSize: 14,
    marginBottom: 12,
  },
  row: { flexDirection: 'row', gap: 10 },
  saveBtn: {
    flex: 1,
    backgroundColor: '#10B981',
    padding: 12,
    borderRadius: 8,
    alignItems: 'center',
  },
  saveBtnText: { color: '#020617', fontWeight: '700', fontSize: 14 },
  resetBtn: {
    backgroundColor: '#1E293B',
    paddingVertical: 12,
    paddingHorizontal: 16,
    borderRadius: 8,
    justifyContent: 'center',
  },
  resetBtnText: { color: '#E2E8F0', fontWeight: '600', fontSize: 14 },
  statusText: { marginTop: 12, fontSize: 13, fontWeight: '600' },
});