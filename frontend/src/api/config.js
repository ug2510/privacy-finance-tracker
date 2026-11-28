import AsyncStorage from '@react-native-async-storage/async-storage';
import { Platform } from 'react-native';

const STORAGE_KEY = 'PRIVACY_FINANCE_API_BASE_URL';

export const DEFAULT_API_BASE_URL =
  Platform.OS === 'web'
    ? 'http://localhost:8080/api/v1'
    : 'http://192.168.1.109:8080/api/v1';

export async function getApiBaseUrl() {
  try {
    const saved = await AsyncStorage.getItem(STORAGE_KEY);
    return saved && saved.trim().length > 0 ? saved.trim() : DEFAULT_API_BASE_URL;
  } catch {
    return DEFAULT_API_BASE_URL;
  }
}

export async function setApiBaseUrl(url) {
  const cleaned = url.trim().replace(/\/+$/, '');
  await AsyncStorage.setItem(STORAGE_KEY, cleaned);
}

export async function resetApiBaseUrl() {
  await AsyncStorage.removeItem(STORAGE_KEY);
  return DEFAULT_API_BASE_URL;
}