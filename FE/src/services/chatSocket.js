import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class ChatSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.connecting = false;
        this.retryCount = 0;
        this.maxRetries = 5;
        this.subscriptions = new Map(); // topic -> Set of callbacks
        this.stompSubscriptions = new Map(); // topic -> StompSubscription
        this.localBridge = new BroadcastChannel('aerostride_chat_local');

        // Bridge listener for same-browser multi-tab synchronization
        this.localBridge.addEventListener('message', (event) => {
            const { topic, payload } = event.data || {};
            if (topic && this.subscriptions.has(topic)) {
                const callbacks = this.subscriptions.get(topic);
                callbacks.forEach((cb) => {
                    try {
                        cb(payload);
                    } catch (e) {
                        console.error('Error in local bridge callback:', e);
                    }
                });
            }
        });
    }

    getWsUrl() {
        const envWs = import.meta.env.VITE_WS_URL;
        if (envWs) {
            let cleanUrl = envWs.replace(/^wss:/i, 'https:').replace(/^ws:/i, 'http:');
            return cleanUrl;
        }
        if (typeof window !== 'undefined' && window.location) {
            const proto = window.location.protocol;
            const host = window.location.host;
            return `${proto}//${host}/ws-chat`;
        }
        return '/ws-chat';
    }

    connect(onConnectedCallback) {
        if (onConnectedCallback && this.connected) {
            onConnectedCallback();
        }

        if (this.connecting || this.connected || (this.client && this.client.active)) {
            return;
        }

        this.connecting = true;
        const endpoint = this.getWsUrl();

        this.client = new Client({
            webSocketFactory: () =>
                new SockJS(endpoint, null, {
                    transports: ['websocket', 'xhr-streaming', 'xhr-polling']
                }),
            reconnectDelay: 2500, // Reconnect quickly (2.5s) instead of 10s to prevent missing messages on latency drops
            heartbeatIncoming: 15000,
            heartbeatOutgoing: 15000,
            connectionTimeout: 15000,
            onConnect: () => {
                this.connected = true;
                this.connecting = false;
                this.retryCount = 0;

                // Re-subscribe all active topics
                this.resubscribeAll();

                if (onConnectedCallback) {
                    onConnectedCallback();
                }

                // Notify components to sync/fetch any messages missed during disconnected state
                if (typeof window !== 'undefined') {
                    window.dispatchEvent(new CustomEvent('chat-socket-reconnected'));
                }
            },
            onDisconnect: () => {
                this.connected = false;
                this.connecting = false;
                this.stompSubscriptions.clear();
            },
            onStompError: (frame) => {
                this.connecting = false;
                console.warn('Chat Socket Stomp Error:', frame?.headers?.message || 'Unknown');
            },
            onWebSocketError: () => {
                this.connecting = false;
                this.retryCount++;
                if (this.retryCount > this.maxRetries && this.client) {
                    this.client.reconnectDelay = 5000; // max 5s backoff
                }
            }
        });

        this.client.activate();
    }

    resubscribeAll() {
        if (!this.client || !this.connected) return;

        this.subscriptions.forEach((callbacks, topic) => {
            if (callbacks.size > 0 && !this.stompSubscriptions.has(topic)) {
                try {
                    const stompSub = this.client.subscribe(topic, (message) => {
                        try {
                            const data = JSON.parse(message.body);
                            const topicCallbacks = this.subscriptions.get(topic);
                            if (topicCallbacks) {
                                topicCallbacks.forEach((cb) => cb(data));
                            }
                        } catch (e) {
                            console.error('Error parsing STOMP message for topic:', topic, e);
                        }
                    });
                    this.stompSubscriptions.set(topic, stompSub);
                } catch (err) {
                    console.error('Error subscribing to topic:', topic, err);
                }
            }
        });
    }

    subscribe(topic, callback) {
        if (!this.subscriptions.has(topic)) {
            this.subscriptions.set(topic, new Set());
        }
        this.subscriptions.get(topic).add(callback);

        // If STOMP is already connected and this topic is not yet subscribed on client
        if (this.client && this.connected && !this.stompSubscriptions.has(topic)) {
            try {
                const stompSub = this.client.subscribe(topic, (message) => {
                    try {
                        const data = JSON.parse(message.body);
                        const topicCallbacks = this.subscriptions.get(topic);
                        if (topicCallbacks) {
                            topicCallbacks.forEach((cb) => cb(data));
                        }
                    } catch (e) {
                        console.error('Error parsing STOMP message for topic:', topic, e);
                    }
                });
                this.stompSubscriptions.set(topic, stompSub);
            } catch (err) {
                console.error('Error subscribing to topic on STOMP:', topic, err);
            }
        }

        // Return unsubscribe handle
        return {
            unsubscribe: () => {
                const cbs = this.subscriptions.get(topic);
                if (cbs) {
                    cbs.delete(callback);
                    if (cbs.size === 0) {
                        this.subscriptions.delete(topic);
                        const stompSub = this.stompSubscriptions.get(topic);
                        if (stompSub) {
                            try {
                                stompSub.unsubscribe();
                            } catch (e) {
                                // Ignore
                            }
                            this.stompSubscriptions.delete(topic);
                        }
                    }
                }
            }
        };
    }

    send(destination, payload) {
        if (this.client && this.connected) {
            this.client.publish({
                destination: destination,
                body: JSON.stringify(payload)
            });
        }

        // Broadcast to other local tabs
        this.localBridge.postMessage({
            topic: destination.replace('/app/', '/topic/'),
            payload: { ...payload, id: Date.now(), time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
        });
    }

    disconnect() {
        if (this.client) {
            try {
                this.client.deactivate();
            } catch (e) {
                // Ignore
            }
        }
        this.connected = false;
        this.connecting = false;
        this.stompSubscriptions.clear();
    }
}

export const chatSocket = new ChatSocketService();
