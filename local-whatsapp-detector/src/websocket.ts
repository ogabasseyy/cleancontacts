/**
 * WebSocket Manager for Real-Time Pairing Events
 * 2026 Best Practice: Push events instantly instead of polling
 */
import { WebSocketServer, WebSocket } from 'ws'
import type { Server } from 'http'

interface PairingSession {
  ws: WebSocket
  phoneNumber: string
  state: 'waiting' | 'pairing' | 'connected' | 'failed'
  createdAt: number
}

export class WebSocketManager {
  private wss: WebSocketServer
  private sessions: Map<string, PairingSession> = new Map()

  constructor(server: Server) {
    this.wss = new WebSocketServer({ server, path: '/ws/pairing' })

    this.wss.on('connection', (ws) => {
      console.log('🔌 WebSocket client connected')

      ws.on('message', (data) => {
        try {
          const msg = JSON.parse(data.toString())
          console.log('📩 WebSocket message:', msg.type)

          if (msg.type === 'start_pairing' && msg.phoneNumber) {
            const phone = this.normalizePhone(msg.phoneNumber)
            this.sessions.set(phone, {
              ws,
              phoneNumber: phone,
              state: 'waiting',
              createdAt: Date.now()
            })
            console.log(`📱 Pairing session started for ${phone}`)
            this.emit(phone, 'session_created', { phoneNumber: phone })
          }
        } catch (err) {
          console.error('❌ WebSocket message parse error:', err)
        }
      })

      ws.on('close', () => {
        console.log('🔌 WebSocket client disconnected')
        // Find and remove session for this ws
        for (const [phone, session] of this.sessions.entries()) {
          if (session.ws === ws) {
            this.sessions.delete(phone)
            console.log(`📱 Pairing session ended for ${phone}`)
            break
          }
        }
      })

      ws.on('error', (err) => {
        console.error('❌ WebSocket error:', err)
      })
    })

    console.log('✅ WebSocket server initialized at /ws/pairing')
  }

  private normalizePhone(phone: string): string {
    return phone.replace(/[^0-9]/g, '')
  }

  /**
   * Called by WhatsAppManager when pairing code is generated
   */
  notifyPairingCode(phone: string, code: string) {
    const normalizedPhone = this.normalizePhone(phone)
    console.log(`🔑 Notifying pairing code for ${normalizedPhone}: ${code}`)
    
    // Try to find session by phone number
    const session = this.sessions.get(normalizedPhone)
    if (session) {
      session.state = 'pairing'
      this.emit(normalizedPhone, 'pairing_code', { code })
    } else {
      console.log(`⚠️ No WebSocket session found for ${normalizedPhone}`)
    }
  }

  /**
   * Called by WhatsAppManager when WhatsApp connects successfully
   */
  notifyConnected(phone?: string) {
    console.log('✅ Notifying all sessions of connection')
    
    // Notify all active sessions
    for (const [sessionPhone, session] of this.sessions.entries()) {
      session.state = 'connected'
      this.emit(sessionPhone, 'connected', {})
    }
  }

  /**
   * Called by WhatsAppManager on errors
   */
  notifyError(phone: string, error: string) {
    const normalizedPhone = this.normalizePhone(phone)
    const session = this.sessions.get(normalizedPhone)
    if (session) {
      session.state = 'failed'
      this.emit(normalizedPhone, 'error', { error })
    }
  }

  private emit(phone: string, type: string, data: any) {
    const session = this.sessions.get(phone)
    if (session?.ws.readyState === WebSocket.OPEN) {
      const message = JSON.stringify({ type, ...data, timestamp: Date.now() })
      session.ws.send(message)
      console.log(`📤 Sent to ${phone}: ${type}`)
    }
  }

  /**
   * Check if any pairing is in progress (for health check)
   */
  isPairingInProgress(): boolean {
    return this.sessions.size > 0
  }

  /**
   * Get active session count
   */
  getActiveSessionCount(): number {
    return this.sessions.size
  }
}
