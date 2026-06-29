/**
 * AsyncStorage helper utilities
 */

import AsyncStorage from '@react-native-async-storage/async-storage';

const KEYS = {
  ACCESS_TOKEN: '@aerostride_access_token',
  REFRESH_TOKEN: '@aerostride_refresh_token',
  USER: '@aerostride_user',
  CART: '@aerostride_cart',
  CHAT_SESSION: '@aerostride_chat_session',
} as const;

export const storage = {
  // Token management
  async getAccessToken(): Promise<string | null> {
    return AsyncStorage.getItem(KEYS.ACCESS_TOKEN);
  },

  async setAccessToken(token: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.ACCESS_TOKEN, token);
  },

  async getRefreshToken(): Promise<string | null> {
    return AsyncStorage.getItem(KEYS.REFRESH_TOKEN);
  },

  async setRefreshToken(token: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.REFRESH_TOKEN, token);
  },

  // User management
  async getUser(): Promise<{ username: string; role: string } | null> {
    const raw = await AsyncStorage.getItem(KEYS.USER);
    return raw ? JSON.parse(raw) : null;
  },

  async setUser(user: { username: string; role: string }): Promise<void> {
    await AsyncStorage.setItem(KEYS.USER, JSON.stringify(user));
  },

  // Cart management
  async getCart(): Promise<any[]> {
    const raw = await AsyncStorage.getItem(KEYS.CART);
    return raw ? JSON.parse(raw) : [];
  },

  async setCart(items: any[]): Promise<void> {
    await AsyncStorage.setItem(KEYS.CART, JSON.stringify(items));
  },

  // Chat session (persisted so guest conversations survive app restarts)
  async getChatSessionId(): Promise<string | null> {
    return AsyncStorage.getItem(KEYS.CHAT_SESSION);
  },

  async setChatSessionId(sessionId: string): Promise<void> {
    await AsyncStorage.setItem(KEYS.CHAT_SESSION, sessionId);
  },

  // Clear all auth data
  async clearAuth(): Promise<void> {
    await Promise.all([
      AsyncStorage.removeItem(KEYS.ACCESS_TOKEN),
      AsyncStorage.removeItem(KEYS.REFRESH_TOKEN),
      AsyncStorage.removeItem(KEYS.USER),
    ]);
  },

  async clearAll(): Promise<void> {
    await Promise.all([
      AsyncStorage.removeItem(KEYS.ACCESS_TOKEN),
      AsyncStorage.removeItem(KEYS.REFRESH_TOKEN),
      AsyncStorage.removeItem(KEYS.USER),
      AsyncStorage.removeItem(KEYS.CART),
    ]);
  },
};
