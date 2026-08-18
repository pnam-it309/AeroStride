import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.connecting = false;
        this.subscriptions = new Map();
        this.retryCount = 0;
        this.maxRetries = 5;
    }

    connect(onMessageCallback) {
        if (this.connected || this.connecting || (this.stompClient && this.stompClient.active)) {
            return;
        }

        this.connecting = true;

        let rawUrl = import.meta.env.VITE_WS_URL || '/ws';
        let httpUrl = rawUrl.replace(/^wss:/i, 'https:').replace(/^ws:/i, 'http:');
        if (!httpUrl.startsWith('http') && !httpUrl.startsWith('/')) {
            httpUrl = '/ws';
        }

        this.stompClient = new Client({
            webSocketFactory: () => new SockJS(httpUrl),
            reconnectDelay: 10000, // 10s backoff to avoid rate limiting (429)
            heartbeatIncoming: 10000,
            heartbeatOutgoing: 10000,
            connectionTimeout: 10000
        });

        this.stompClient.onConnect = (frame) => {
            this.connected = true;
            this.connecting = false;
            this.retryCount = 0;

            // Default global notifications
            this.subscribe('/topic/notifications', onMessageCallback);
            this.subscribe('/topic/messages', onMessageCallback);
            this.subscribe('/topic/product-stock', onMessageCallback);

            // Subscribe to private notifications if user is logged in
            try {
                const user = JSON.parse(localStorage.getItem('user'));
                if (user && user.username) {
                    this.subscribe(`/user/${user.username}/queue/notifications`, onMessageCallback);
                }
            } catch (e) {
                // Ignore parse error
            }
        };

        this.stompClient.onDisconnect = () => {
            this.connected = false;
            this.connecting = false;
        };

        this.stompClient.onStompError = (frame) => {
            this.connecting = false;
            console.warn('WebSocket Stomp error:', frame?.headers?.message || 'Unknown error');
        };

        this.stompClient.onWebSocketError = () => {
            this.connecting = false;
            this.retryCount++;
            if (this.retryCount > this.maxRetries) {
                // Stop hammering if server is down or rate limited
                if (this.stompClient) {
                    this.stompClient.reconnectDelay = 30000; // back off to 30s
                }
            }
        };

        this.stompClient.activate();
    }

    subscribe(destination, callback) {
        if (!this.stompClient || !this.connected) {
            return;
        }

        try {
            const subscription = this.stompClient.subscribe(destination, (message) => {
                try {
                    callback(JSON.parse(message.body));
                } catch (e) {
                    callback(message.body);
                }
            });

            this.subscriptions.set(destination, subscription);
            return subscription;
        } catch (e) {
            console.warn('Error subscribing to', destination, e);
        }
    }

    unsubscribe(destination) {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            try {
                subscription.unsubscribe();
            } catch (e) {
                // Ignore
            }
            this.subscriptions.delete(destination);
        }
    }

    disconnect() {
        if (this.stompClient !== null) {
            try {
                this.stompClient.deactivate();
            } catch (e) {
                // Ignore
            }
        }
        this.connected = false;
        this.connecting = false;
        this.subscriptions.clear();
    }
}

export default new WebSocketService();
