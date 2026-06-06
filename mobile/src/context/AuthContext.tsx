/**
 * Authentication Context for AeroStride Mobile
 */

import React, { createContext, useContext, useEffect, useState, useCallback } from 'react';
import { authService, AuthResponse, LoginRequest } from '@/services/authService';
import { storage } from '@/utils/storage';

interface AuthState {
  user: { username: string; role: string } | null;
  isAuthenticated: boolean;
  isLoading: boolean;
}

interface AuthContextType extends AuthState {
  login: (data: LoginRequest) => Promise<AuthResponse>;
  logout: () => Promise<void>;
  checkAuth: () => Promise<void>;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [state, setState] = useState<AuthState>({
    user: null,
    isAuthenticated: false,
    isLoading: true,
  });

  const checkAuth = useCallback(async () => {
    try {
      const [token, user] = await Promise.all([
        storage.getAccessToken(),
        storage.getUser(),
      ]);
      setState({
        user: token ? user : null,
        isAuthenticated: !!token,
        isLoading: false,
      });
    } catch {
      setState({ user: null, isAuthenticated: false, isLoading: false });
    }
  }, []);

  useEffect(() => {
    checkAuth();
  }, [checkAuth]);

  const login = useCallback(async (data: LoginRequest): Promise<AuthResponse> => {
    const authData = await authService.login(data);
    setState({
      user: { username: authData.username, role: authData.role },
      isAuthenticated: true,
      isLoading: false,
    });
    return authData;
  }, []);

  const logout = useCallback(async () => {
    await authService.logout();
    setState({
      user: null,
      isAuthenticated: false,
      isLoading: false,
    });
  }, []);

  return (
    <AuthContext.Provider value={{ ...state, login, logout, checkAuth }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextType {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
}
