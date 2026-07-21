/**
 * Chat Service - matches BE CustomerChatController (public endpoints)
 * The BE auto-replies via an AI assistant when no staff has picked up the
 * conversation, so the mobile client sends a message then polls /history.
 */

import apiClient from './apiClient';
import { API_PATHS } from '@/config/api';
import { storage } from '@/utils/storage';

export const SENDER_CUSTOMER = 'customer';

// Matches BE CustomerTinNhanResponse (with @JsonProperty aliases)
export interface ChatMessage {
  id: string;
  conversationId?: string;
  sessionId?: string;
  sender: string; // 'customer' | 'staff' | 'system'
  text: string;
  time?: string;
  staffId?: string;
  secondStaffId?: string;
}

interface ChatEnvelope<T> {
  success: boolean;
  data: T;
}

const generateSessionId = (): string =>
  `mobile-${Date.now()}-${Math.random().toString(36).slice(2, 10)}`;

export const chatService = {
  /** Get a stable session id for this device, creating one on first use. */
  async getSessionId(): Promise<string> {
    let sessionId = await storage.getChatSessionId();
    if (!sessionId) {
      sessionId = generateSessionId();
      await storage.setChatSessionId(sessionId);
    }
    return sessionId;
  },

  async getHistory(sessionId: string): Promise<ChatMessage[]> {
    const response = await apiClient.get<ChatEnvelope<ChatMessage[]>>(
      API_PATHS.CHAT.HISTORY,
      { params: { sessionId } }
    );
    return response.data.data || [];
  },

  async sendMessage(sessionId: string, text: string): Promise<void> {
    await apiClient.post(API_PATHS.CHAT.SEND, {
      sessionId,
      text,
      sender: SENDER_CUSTOMER,
    });
  },

  async getWelcomeSuggestions(sessionId: string): Promise<string[]> {
    try {
      const response = await apiClient.get<ChatEnvelope<string[]>>(
        API_PATHS.CHAT.WELCOME_SUGGESTIONS,
        { params: { sessionId } }
      );
      return response.data.data || [];
    } catch {
      return [];
    }
  },
};
