import { defineStore } from 'pinia'
import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { ref } from 'vue'

export const useNotificationStore = defineStore('notification', () => {
  const notifications = ref([])
  const isConnected = ref(false)
  let stompClient = null

  const connect = () => {
    const wsUrl = import.meta.env.VITE_WS_URL || 'http://localhost:8080/ws'
    const socket = new SockJS(wsUrl)
    stompClient = new Client({
      webSocketFactory: () => socket,
      reconnectDelay: 5000,
      onConnect: () => {
        isConnected.value = true
        stompClient.subscribe('/topic/notifications', (message) => {
          const notification = JSON.parse(message.body)
          notifications.value.push(notification)
        })
      },
      onDisconnect: () => {
        isConnected.value = false
      },
    })

    stompClient.activate()
  }

  const subscribeToUser = (username) => {
    if (!stompClient) return
    stompClient.subscribe(`/user/${username}/queue/notifications`, (message) => {
      const notification = JSON.parse(message.body)
      notifications.value.push(notification)
    })
  }

  const sendMessage = (text) => {
    if (stompClient && isConnected.value) {
      stompClient.publish({
        destination: '/app/send-notification',
        body: JSON.stringify(text),
      })
    }
  }

  const disconnect = () => {
    if (stompClient) {
      stompClient.deactivate()
    }
  }

  return {
    notifications,
    isConnected,
    connect,
    subscribeToUser,
    sendMessage,
    disconnect,
  }
})
