import { Client } from '@stomp/stompjs'
import SockJS from 'sockjs-client'
import { API_BASE } from './api'

export function connectSpaceRealtime(spaceId, onEvent) {
  let active = true
  let pollTimer = null
  let client = null

  const startPolling = () => {
    if (pollTimer || !active) return
    pollTimer = setInterval(() => {
      onEvent({ type: 'POLL' })
    }, 3000)
  }

  try {
    client = new Client({
      webSocketFactory: () => new SockJS(`${API_BASE}/ws`),
      reconnectDelay: 4000,
      onConnect: () => {
        if (pollTimer) {
          clearInterval(pollTimer)
          pollTimer = null
        }
        client.subscribe(`/topic/space/${spaceId}`, (message) => {
          try {
            onEvent(JSON.parse(message.body))
          } catch {
            onEvent({ type: 'POLL' })
          }
        })
      },
      onStompError: startPolling,
      onWebSocketClose: () => {
        if (active) startPolling()
      },
    })
    client.activate()
    setTimeout(() => {
      if (active && (!client.connected)) startPolling()
    }, 2500)
  } catch {
    startPolling()
  }

  return () => {
    active = false
    if (pollTimer) clearInterval(pollTimer)
    if (client) client.deactivate()
  }
}
