import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class ChatSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
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
        return '/ws-chat';
    }

    connect(onConnectedCallback) {
        if (onConnectedCallback && this.connected) {
            onConnectedCallback();
        }

        if (this.client && (this.client.active || this.connected)) {
            return;
        }

        const endpoint = this.getWsUrl();

        this.client = new Client({
            webSocketFactory: () =>
                new SockJS(endpoint, null, {
                    transports: ['websocket', 'xhr-streaming', 'xhr-polling']
                }),
            reconnectDelay: 2500,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
            onConnect: () => {
                console.log('✅ Chat WebSocket (STOMP) connected to', endpoint);
                this.connected = true;

                // Re-subscribe all active topics
                this.resubscribeAll();

                if (onConnectedCallback) {
                    onConnectedCallback();
                }
            },
            onDisconnect: () => {
                this.connected = false;
                this.stompSubscriptions.clear();
            },
            onStompError: (frame) => {
                console.warn('Socket Stomp Error:', frame.headers ? frame.headers['message'] : frame);
            },
            onWebSocketError: (event) => {
                console.warn('WebSocket transport error, will auto-reconnect:', event);
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
                            stompSub.unsubscribe();
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
            this.client.deactivate();
        }
        this.connected = false;
        this.stompSubscriptions.clear();
    }
}

export const chatSocket = new ChatSocketService();
