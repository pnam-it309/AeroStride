import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class ChatSocketService {
    constructor() {
        this.client = null;
        this.connected = false;
        this.subscriptions = new Map();
        // Local bridge for cross-tab testing without a real backend socket server
        this.localBridge = new BroadcastChannel('aerostride_chat_local');
    }

    connect(onConnectedCallback) {
        // Only try to connect if VITE_API_URL is available and looks like a real URL
        const apiUrl = import.meta.env.VITE_API_URL;
        if (!apiUrl || apiUrl.includes('localhost:5173')) {
            console.warn('Real WebSocket skipped (Local Bridge active). Using BroadcastChannel for testing.');
            this.connected = true;
            if (onConnectedCallback) onConnectedCallback();
            return;
        }

        const wsUrl = import.meta.env.VITE_WS_URL || '/ws-chat';
        // Note: chatSocket uses /ws-chat by default in backend
        const socket = new SockJS(wsUrl.includes('ws-chat') ? wsUrl : '/ws-chat');
        this.client = new Client({
            webSocketFactory: () => socket,
            onConnect: () => {
                this.connected = true;
                if (onConnectedCallback) onConnectedCallback();
            },
            onStompError: (frame) => {
                console.error('Socket Error:', frame.headers['message']);
            }
        });

        this.client.activate();
    }

    subscribe(topic, callback) {
        // Real subscription
        let stompSub = null;
        if (this.client && this.connected) {
            stompSub = this.client.subscribe(topic, (message) => {
                if (callback) callback(JSON.parse(message.body));
            });
        }

        // Local bridge listener
        const bridgeListener = (event) => {
            if (event.data.topic === topic) {
                callback(event.data.payload);
            }
        };
        this.localBridge.addEventListener('message', bridgeListener);

        return {
            unsubscribe: () => {
                if (stompSub) stompSub.unsubscribe();
                this.localBridge.removeEventListener('message', bridgeListener);
            }
        };
    }

    send(destination, payload) {
        // Send to real backend
        if (this.client && this.connected) {
            this.client.publish({
                destination: destination,
                body: JSON.stringify(payload)
            });
        }

        // Broadcast to other local tabs/components
        // We simulate the topic by mapping destination to topic
        // e.g. /app/chat.send -> /topic/messages
        this.localBridge.postMessage({
            topic: destination.replace('/app/', '/topic/'), 
            payload: { ...payload, id: Date.now(), time: new Date().toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) }
        });
    }

    disconnect() {
        if (this.client) this.client.deactivate();
        this.connected = false;
    }
}

export const chatSocket = new ChatSocketService();
