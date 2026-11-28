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
import {
  fetchTransactions,
  createTransaction,
  triggerCategorization,
} from '../../src/api/financeApi';

export default function ExpensesScreen() {
  const [transactions, setTransactions] = useState([]);
  const [loading, setLoading] = useState(true);
  const [refreshing, setRefreshing] = useState(false);
  const [categorising, setCategorising] = useState(false);
  const [error, setError] = useState('');

  // Quick manual entry state
  const [merchant, setMerchant] = useState('');
  const [amount, setAmount] = useState('');
  const [type, setType] = useState('DEBIT');

  const loadData = useCallback(async () => {
    try {
      setError('');
      const data = await fetchTransactions();
      setTransactions(Array.isArray(data) ? data : []);
    } catch (err) {
      setError(err.message || 'Could not reach backend');
    } finally {
      setLoading(false);
      setRefreshing(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleAddTransaction = async () => {
    const parsedAmount = parseFloat(amount);
    if (!merchant.trim() || isNaN(parsedAmount) || parsedAmount <= 0) {
      setError('Enter a valid merchant name and positive amount.');
      return;
    }
    try {
      setError('');
      await createTransaction({
        merchant: merchant.trim(),
        amount: parsedAmount,
        type,
        rawSms: `Manual entry: ${type} INR ${parsedAmount} at ${merchant.trim()}`,
      });
      setMerchant('');
      setAmount('');
      await loadData();
    } catch (err) {
      setError(err.message);
    }
  };

  const handleCategorise = async () => {
    try {
      setCategorising(true);
      setError('');
      await triggerCategorization();
      await loadData();
    } catch (err) {
      setError(err.message);
    } finally {
      setCategorising(false);
    }
  };

  const totalDebit = transactions
    .filter((t) => t.type === 'DEBIT')
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0);

  const totalCredit = transactions
    .filter((t) => t.type === 'CREDIT')
    .reduce((sum, t) => sum + (Number(t.amount) || 0), 0);

  return (
    <ScrollView
      style={styles.container}
      contentContainerStyle={styles.content}
      refreshControl={
        <RefreshControl
          refreshing={refreshing}
          onRefresh={() => {
            setRefreshing(true);
            loadData();
          }}
          tintColor="#10B981"
        />
      }
    >
      {/* Summary Cards */}
      <View style={styles.summaryRow}>
        <View style={[styles.summaryCard, { borderColor: '#EF4444' }]}>
          <Text style={styles.summaryLabel}>Total Spent</Text>
          <Text style={[styles.summaryValue, { color: '#F87171' }]}>
            ₹{totalDebit.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
          </Text>
        </View>
        <View style={[styles.summaryCard, { borderColor: '#10B981' }]}>
          <Text style={styles.summaryLabel}>Total Income</Text>
          <Text style={[styles.summaryValue, { color: '#34D399' }]}>
            ₹{totalCredit.toLocaleString('en-IN', { maximumFractionDigits: 2 })}
          </Text>
        </View>
      </View>

      {/* AI Categorization Banner */}
      <TouchableOpacity
        style={styles.aiButton}
        onPress={handleCategorise}
        disabled={categorising}
      >
        {categorising ? (
          <ActivityIndicator color="#0F172A" />
        ) : (
          <Text style={styles.aiButtonText}>
            ✨ Auto-Categorize Unlabelled Transactions (Gemini AI)
          </Text>
        )}
      </TouchableOpacity>

      {/* Quick Add Form */}
      <View style={styles.card}>
        <Text style={styles.sectionTitle}>Add Transaction</Text>
        <View style={styles.inputRow}>
          <TextInput
            style={[styles.input, { flex: 2 }]}
            placeholder="Merchant (e.g. Swiggy, Uber)"
            placeholderTextColor="#64748B"
            value={merchant}
            onChangeText={setMerchant}
          />
          <TextInput
            style={[styles.input, { flex: 1 }]}
            placeholder="₹ Amount"
            placeholderTextColor="#64748B"
            keyboardType="numeric"
            value={amount}
            onChangeText={setAmount}
          />
        </View>
        <View style={styles.actionRow}>
          <View style={styles.typeToggle}>
            {['DEBIT', 'CREDIT'].map((item) => (
              <TouchableOpacity
                key={item}
                style={[
                  styles.typeChip,
                  type === item && styles.typeChipActive,
                ]}
                onPress={() => setType(item)}
              >
                <Text
                  style={[
                    styles.typeChipText,
                    type === item && styles.typeChipTextActive,
                  ]}
                >
                  {item}
                </Text>
              </TouchableOpacity>
            ))}
          </View>
          <TouchableOpacity style={styles.addButton} onPress={handleAddTransaction}>
            <Text style={styles.addButtonText}>+ Save</Text>
          </TouchableOpacity>
        </View>
      </View>

      {error ? <Text style={styles.errorText}>{error}</Text> : null}

      {/* Transactions Feed */}
      <Text style={styles.sectionTitle}>
        Recent Transactions ({transactions.length})
      </Text>

      {loading ? (
        <ActivityIndicator size="large" color="#10B981" style={{ marginTop: 24 }} />
      ) : transactions.length === 0 ? (
        <Text style={styles.emptyText}>
          No transactions yet. Add one above or sync bank SMS from Android.
        </Text>
      ) : (
        transactions.map((tx, idx) => (
          <View key={tx.id || idx} style={styles.txCard}>
            <View style={{ flex: 1 }}>
              <Text style={styles.txMerchant}>{tx.merchant || 'Unknown Merchant'}</Text>
              <View style={styles.badgeRow}>
                <View style={styles.categoryBadge}>
                  <Text style={styles.categoryText}>
                    {tx.category || 'Uncategorized'}
                  </Text>
                </View>
                {tx.transactionDate ? (
                  <Text style={styles.dateText}>{tx.transactionDate}</Text>
                ) : null}
              </View>
            </View>
            <Text
              style={[
                styles.txAmount,
                { color: tx.type === 'CREDIT' ? '#34D399' : '#F8FAFC' },
              ]}
            >
              {tx.type === 'CREDIT' ? '+' : '-'}₹
              {Number(tx.amount).toLocaleString('en-IN', {
                maximumFractionDigits: 2,
              })}
            </Text>
          </View>
        ))
      )}
    </ScrollView>
  );
}

const styles = StyleSheet.create({
  container: { flex: 1, backgroundColor: '#020617' },
  content: { padding: 16, maxWidth: 800, width: '100%', alignSelf: 'center' },
  summaryRow: { flexDirection: 'row', gap: 12, marginBottom: 14 },
  summaryCard: {
    flex: 1,
    backgroundColor: '#0F172A',
    padding: 16,
    borderRadius: 12,
    borderWidth: 1,
  },
  summaryLabel: { color: '#94A3B8', fontSize: 13, marginBottom: 4 },
  summaryValue: { fontSize: 22, fontWeight: '700' },
  aiButton: {
    backgroundColor: '#10B981',
    padding: 14,
    borderRadius: 10,
    alignItems: 'center',
    marginBottom: 16,
  },
  aiButtonText: { color: '#020617', fontWeight: '700', fontSize: 14 },
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
    marginBottom: 10,
  },
  inputRow: { flexDirection: 'row', gap: 10, marginBottom: 10 },
  input: {
    backgroundColor: '#1E293B',
    color: '#F8FAFC',
    paddingHorizontal: 12,
    paddingVertical: 10,
    borderRadius: 8,
    fontSize: 14,
  },
  actionRow: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
  },
  typeToggle: { flexDirection: 'row', gap: 8 },
  typeChip: {
    paddingVertical: 6,
    paddingHorizontal: 12,
    borderRadius: 6,
    backgroundColor: '#1E293B',
  },
  typeChipActive: { backgroundColor: '#334155', borderWidth: 1, borderColor: '#10B981' },
  typeChipText: { color: '#94A3B8', fontSize: 12, fontWeight: '600' },
  typeChipTextActive: { color: '#10B981' },
  addButton: {
    backgroundColor: '#2563EB',
    paddingVertical: 8,
    paddingHorizontal: 18,
    borderRadius: 8,
  },
  addButtonText: { color: '#FFFFFF', fontWeight: '700', fontSize: 14 },
  errorText: { color: '#F87171', marginBottom: 12, fontSize: 13 },
  emptyText: { color: '#64748B', textAlign: 'center', marginTop: 24 },
  txCard: {
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: '#0F172A',
    padding: 14,
    borderRadius: 10,
    borderWidth: 1,
    borderColor: '#1E293B',
    marginBottom: 10,
  },
  txMerchant: { color: '#F8FAFC', fontSize: 15, fontWeight: '600', marginBottom: 6 },
  badgeRow: { flexDirection: 'row', alignItems: 'center', gap: 8 },
  categoryBadge: {
    backgroundColor: '#1E293B',
    paddingHorizontal: 8,
    paddingVertical: 3,
    borderRadius: 6,
  },
  categoryText: { color: '#38BDF8', fontSize: 11, fontWeight: '600' },
  dateText: { color: '#64748B', fontSize: 11 },
  txAmount: { fontSize: 16, fontWeight: '700' },
});