/**
 * Chat WebSocket client (STOMP over raw WebSocket).
 *
 * BE exposes a SockJS endpoint at `/ws-chat` and broadcasts every chat message
 * (customer, AI and staff) to the `/topic/messages` STOMP topic. SockJS itself is
 * not React-Native friendly, but its raw WebSocket sub-endpoint
 * (`/ws-chat/websocket`) speaks plain WebSocket, which `@stomp/stompjs` can drive
 * directly using RN's global WebSocket.
 *
 * Sending still goes through the REST endpoint (chatService.sendMessage); this
 * socket is only used to receive live updates. Polling remains as a fallback when
 * the socket cannot connect.
 */

import { Client, type IMessage } from '@stomp/stompjs';
import { API_CONFIG } from '@/config/api';
import type { ChatMessage } from './chatService';

const TOPIC_MESSAGES = '/topic/messages';

export interface ChatSocketHandlers {
  onMessage: (message: ChatMessage) => void;
  onStatusChange?: (connected: boolean) => void;
}

export class ChatSocket {
  private client: Client | null = null;

  connect(handlers: ChatSocketHandlers) {
    if (this.client) return;

    const client = new Client({
      brokerURL: API_CONFIG.CHAT_WS_URL,
      reconnectDelay: 4000,
      heartbeatIncoming: 10000,
      heartbeatOutgoing: 10000,
      onConnect: () => {
        handlers.onStatusChange?.(true);
        client.subscribe(TOPIC_MESSAGES, (frame: IMessage) => {
          try {
            const payload = JSON.parse(frame.body) as ChatMessage;
            handlers.onMessage(payload);
          } catch {
            // ignore malformed frames
          }
        });
      },
      onWebSocketClose: () => handlers.onStatusChange?.(false),
      onStompError: () => handlers.onStatusChange?.(false),
    });

    // Silence verbose internal logging
    client.debug = () => {};
    this.client = client;
    client.activate();
  }

  disconnect() {
    if (this.client) {
      this.client.deactivate();
      this.client = null;
    }
  }
}
