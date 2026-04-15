import SockJS from 'sockjs-client';
import { Client } from '@stomp/stompjs';

class WebSocketService {
    constructor() {
        this.stompClient = null;
        this.connected = false;
        this.subscriptions = new Map();
        this.baseUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws';
    }

    connect(onMessageCallback) {
        if (this.connected) return;

        const socket = new SockJS(this.baseUrl);
        this.stompClient = new Client({
            webSocketFactory: () => socket,
            reconnectDelay: 5000,
            heartbeatIncoming: 4000,
            heartbeatOutgoing: 4000,
        });

        this.stompClient.onConnect = (frame) => {
            this.connected = true;
            console.log('WebSocket Connected: ' + frame);
            
            // Default global notifications
            this.subscribe('/topic/notifications', onMessageCallback);
            
            // Subscribe to private notifications if user is logged in
            const user = JSON.parse(localStorage.getItem('user'));
            if (user && user.username) {
                this.subscribe(`/user/${user.username}/queue/notifications`, onMessageCallback);
            }
        };

        this.stompClient.onStompError = (frame) => {
            console.error('Broker reported error: ' + frame.headers['message']);
            console.error('Additional details: ' + frame.body);
        };

        this.stompClient.activate();
    }

    subscribe(destination, callback) {
        if (!this.stompClient || !this.connected) {
            console.warn('Cannot subscribe. WebSocket is not connected.');
            return;
        }

        const subscription = this.stompClient.subscribe(destination, (message) => {
            callback(JSON.parse(message.body));
        });
        
        this.subscriptions.set(destination, subscription);
        return subscription;
    }

    unsubscribe(destination) {
        const subscription = this.subscriptions.get(destination);
        if (subscription) {
            subscription.unsubscribe();
            this.subscriptions.delete(destination);
        }
    }

    disconnect() {
        if (this.stompClient !== null) {
            this.stompClient.deactivate();
        }
        this.connected = false;
        console.log('WebSocket Disconnected');
    }
}

export default new WebSocketService();
