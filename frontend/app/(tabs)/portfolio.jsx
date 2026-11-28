import React, { useState, useEffect, useCallback } from 'react';
import {
  View,
  Text,
  StyleSheet,
  ScrollView,
  TouchableOpacity,
  TextInput,
  ActivityIndicator,
  RefreshControl,
} from 'react-native';
import * as DocumentPicker from 'expo-document-picker';
import { fetchPortfolio, uploadCasPdf } from '../../src/api/financeApi';

export default function PortfolioScreen() {
  const [portfolio, setPortfolio] = useState({ totalValue: 0, holdings: [] });
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [uploading, setUploading] = useState(false);

  const [selectedFile, setSelectedFile] = useState(null);
  const [password, setPassword] = useState('');
  const [statusMsg, setStatusMsg] = useState('');
  const [error, setError] = useState('');

  const loadPortfolio = useCallback(async () => {
    try {
      setError('');
      const data = await fetchPortfolio();
      setPortfolio({
        totalValue: data?.totalValue || 0,
        lastSynced: data?.lastSynced,
        holdings: Array.isArray(data?.holdings) ? data.holdings : [],
      });
    } catch (err) {
      setError(err.message || 'Could not load portfolio');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadPortfolio();
  }, [loadPortfolio]);

  const handlePickPdf = async () => {
    try {
      setError('');
      const result = await DocumentPicker.getDocumentAsync({
        type: 'application/pdf',
        copyToCacheDirectory: true,
      });
      if (!result.canceled && result.assets && result.assets.length > 0) {
        setSelectedFile(result.assets[0]);
        setStatusMsg(`Selected: ${result.assets[0].name}`);
      }
    } catch (err) {
      setError('Failed to open file picker: ' + err.message);
    }
  };

  const handleUpload = async () => {
    if (!selectedFile) {
      setError('Please select your NSDL/CAMS/KFintech CAS PDF first.');
      return;
    }
    try {
      setUploading(true);
      setError('');
      setStatusMsg('Decrypting & parsing CAS PDF locally...');
      const updated = await uploadCasPdf(selectedFile, password);
      setPortfolio({
        totalValue: updated?.totalValue || 0,
        lastSynced: updated?.lastSynced,
        holdings: Array.isArray(updated?.holdings) ? updated.holdings : [],
      });
      setStatusMsg(
        `Synced ${updated?.holdings?.length || 0} holdings successfully!`
      );
      setSelectedFile(null);
      setPassword('');
    } catch (err) {
      setStatusMsg('');
      setError(err.message || 'CAS upload failed');
    } finally {
      setUploading(false);
    }
  };

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={() => {
            setRefreshing(true);
            loadPortfolio();
          }}
          tintColor="#10B981"
        />
      }
    >
      {/* Net Portfolio Value Hero */}
      <View style={styles.heroCard}>
        <Text style={styles.heroLabel}>Total Portfolio Valuation</Text>
        <Text style={styles.heroValue}>
          ₹
          {Number(portfolio.totalValue).toLocaleString('en-IN', {
            maximumFractionDigits: 2,
          })}
        </Text>
        <Text style={styles.heroSub}>
          {portfolio.holdings.length} Active Holdings (NSDL / CAMS)
        </Text>
      </View>

      {/* CAS Upload Card */}
      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Import CAS Statement (PDF)</Text>
        <Text style={styles.helperText}>
          Parsed 100% locally on your machine via Python FastAPI. Your PAN
          password is never stored.
        </Text>

        <TouchableOpacity style={styles.pickerButton} onPress={handlePickPdf}>
          <Text style={styles.pickerButtonText}>
            {selectedFile ? `📄 ${selectedFile.name}` : '📁 Choose CAS PDF File'}
          </Text>
        </TouchableOpacity>

        <TextInput
          style={styles.input}
          placeholder="PDF Password (usually PAN in CAPS or DOB)"
          placeholderTextColor="#64748B"
          secureTextEntry
          autoCapitalize="characters"
          value={password}
          onChangeText={setPassword}
        />

        <TouchableOpacity
          style={styles.uploadButton}
          onPress={handleUpload}
          disabled={uploading}
        >
          {uploading ? (
            <ActivityIndicator color="#020617" />
          ) : (
            <Text style={styles.uploadButtonText}>Upload & Sync Holdings</Text>
          )}
        </TouchableOpacity>

        {statusMsg ? <Text style={styles.successText}>{statusMsg}</Text> : null}
        {error ? <Text style={styles.errorText}>{error}</Text> : null}
      </View>

      {/* Holdings List */}
      <Text style={styles.sectionTitle}>Holdings Breakdown</Text>

      {loading ? (
        <ActivityIndicator size="large" color="#10B981" style={{ marginTop: 24 }} />
      ) : portfolio.holdings.length === 0 ? (
        <Text style={styles.emptyText}>
          No holdings synced yet. Upload your monthly NSDL or CAMS CAS PDF above.
        </Text>
      ) : (
        portfolio.holdings.map((h, idx) => (
          <View key={h.id || h.isin || idx} style={styles.holdingCard}>
            <View style={styles.holdingHeader}>
              <Text style={styles.schemeName} numberOfLines={2}>
                {h.schemeName}
              </Text>
              <View style={styles.assetBadge}>
                <Text style={styles.assetBadgeText}>{h.assetType || 'ASSET'}</Text>
              </View>
            </View>

            <Text style={styles.metaText}>
              ISIN: {h.isin} • Folio/DP: {h.folioNumber || 'N/A'}
            </Text>

            <View style={styles.metricsRow}>
              <View>
                <Text style={styles.metricLabel}>Units / Qty</Text>
                <Text style={styles.metricVal}>{Number(h.units).toFixed(3)}</Text>
              </View>
              <View>
                <Text style={styles.metricLabel}>NAV / Price</Text>
                <Text style={styles.metricVal}>
                  ₹{Number(h.nav).toLocaleString('en-IN', { maximumFractionDigits: 2 })}
                </Text>
              </View>
              <View style={{ alignItems: 'flex-end' }}>
                <Text style={styles.metricLabel}>Current Value</Text>
                <Text style={[styles.metricVal, { color: '#34D399' }]}>
                  ₹
                  {Number(h.currentValue).toLocaleString('en-IN', {
                    maximumFractionDigits: 2,
                  })}
                </Text>
              </View>
            </View>
          </View>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#020617' },
  content: { padding: 16, maxWidth: 800, width: '100%', alignSelf: 'center' },
  heroCard: {
    backgroundColor: '#0F172A',
    padding: 20,
    borderRadius: 14,
    borderWidth: 1,
    borderColor: '#10B981',
    marginBottom: 16,
  },
  heroLabel: { color: '#94A3B8', fontSize: 13, marginBottom: 4 },
  heroValue: { color: '#F8FAFC', fontSize: 30, fontWeight: '800' },
  heroSub: { color: '#34D399', fontSize: 12, marginTop: 6 },
  card: {
    backgroundColor: '#0F172A',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
    borderColor: '#1E293B',
    marginBottom: 18,
  },
  sectionTitle: {
    color: '#F8FAFC',
    fontSize: 16,
    fontWeight: '700',
    marginBottom: 8,
  },
  helperText: { color: '#94A3B8', fontSize: 12, marginBottom: 12 },
  pickerButton: {
    backgroundColor: '#1E293B',
    padding: 12,
    borderRadius: 8,
    borderWidth: 1,
    borderStyle: 'dashed',
    borderColor: '#475569',
    alignItems: 'center',
    marginBottom: 10,
  },
  pickerButtonText: { color: '#E2E8F0', fontWeight: '600', fontSize: 13 },
  input: {
    backgroundColor: '#1E293B',
    color: '#F8FAFC',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
    fontSize: 14,
    marginBottom: 12,
  },
  uploadButton: {
    backgroundColor: '#10B981',
    padding: 13,
    borderRadius: 8,
    alignItems: 'center',
  },
  uploadButtonText: { color: '#020617', fontWeight: '700', fontSize: 14 },
  successText: { color: '#34D399', marginTop: 10, fontSize: 13 },
  errorText: { color: '#F87171', marginTop: 10, fontSize: 13 },
  emptyText: { color: '#64748B', textAlign: 'center', marginTop: 24 },
  holdingCard: {
    backgroundColor: '#0F172A',
    padding: 14,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#1E293B',
    marginBottom: 10,
  },
  holdingHeader: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    gap: 8,
    marginBottom: 4,
  },
  schemeName: { color: '#F8FAFC', fontSize: 14, fontWeight: '700', flex: 1 },
  assetBadge: {
    backgroundColor: '#1E293B',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  assetBadgeText: { color: '#A78BFA', fontSize: 10, fontWeight: '700' },
  metaText: { color: '#64748B', fontSize: 11, marginBottom: 10 },
  metricsRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    borderTopWidth: 1,
    borderTopColor: '#1E293B',
    paddingTop: 10,
  },
  metricLabel: { color: '#94A3B8', fontSize: 11, marginBottom: 2 },
  metricVal: { color: '#F8FAFC', fontSize: 14, fontWeight: '700' },
});