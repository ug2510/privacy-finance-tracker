import { Platform } from 'react-native';
import { getApiBaseUrl } from './config';

export async function fetchTransactions() {
  const baseUrl = await getApiBaseUrl();
  const res = await fetch(`${baseUrl}/transactions`);
  if (!res.ok) throw new Error(`Failed to fetch transactions (${res.status})`);
  return res.json();
}

export async function createTransaction(tx) {
  const baseUrl = await getApiBaseUrl();
  const res = await fetch(`${baseUrl}/transactions`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(tx),
  });
  if (!res.ok) throw new Error(`Failed to save transaction (${res.status})`);
  return res.json();
}

export async function triggerCategorization() {
  const baseUrl = await getApiBaseUrl();
  const res = await fetch(`${baseUrl}/transactions/categorise`, {
    method: 'POST',
  });
  if (!res.ok) throw new Error(`Categorization failed (${res.status})`);
  return res.json();
}

export async function fetchPortfolio() {
  const baseUrl = await getApiBaseUrl();
  const res = await fetch(`${baseUrl}/investments`);
  if (!res.ok) throw new Error(`Failed to load portfolio (${res.status})`);
  return res.json();
}

export async function uploadCasPdf(file, password) {
  const baseUrl = await getApiBaseUrl();
  const formData = new FormData();

  if (Platform.OS === 'web') {
    if (file.file) {
      formData.append('file', file.file, file.name);
    } else {
      const blobRes = await fetch(file.uri);
      const blob = await blobRes.blob();
      formData.append('file', blob, file.name);
    }
  } else {
    formData.append('file', {
      uri: file.uri,
      name: file.name,
      type: file.mimeType || 'application/pdf',
    });
  }

  formData.append('password', password);

  const res = await fetch(`${baseUrl}/investments/upload-cas`, {
    method: 'POST',
    body: formData,
  });

  if (!res.ok) {
    const errText = await res.text();
    throw new Error(errText || `CAS upload failed (${res.status})`);
  }
  return res.json();
}

export async function checkBackendHealth() {
  const baseUrl = await getApiBaseUrl();
  const rootUrl = baseUrl.replace(/\/api\/v1$/, '');
  const res = await fetch(`${rootUrl}/actuator/health`);
  if (!res.ok) return false;
  const data = await res.json();
  return data.status === 'UP';
}