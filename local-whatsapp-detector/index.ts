/**
 * WhatsApp Detector Service - Multi-Session
 * Each user gets their own isolated WhatsApp session
 * With WebSocket support for real-time pairing notifications
 */
import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { createServer, IncomingMessage, ServerResponse } from 'http'
import { WebSocketServer, WebSocket } from 'ws'

import { SessionManager } from './session-manager.js'

const app = new Hono()
const sessionManager = new SessionManager()

// Track WebSocket connections by normalized phone number
const wsClients: Map<string, WebSocket> = new Map()

// Logger
const log = (message: string, data?: any) => {
  const timestamp = new Date().toISOString()
  if (data) {
    console.log(`[${timestamp}] ${message}`, data)
  } else {
    console.log(`[${timestamp}] ${message}`)
  }
}

// Normalize phone number for consistent lookup
const normalizePhone = (phone: string): string => {
  return phone.replace(/[^0-9]/g, '')
}

// Send message to WebSocket client
const sendToClient = (phone: string, type: string, data: any) => {
  const normalizedPhone = normalizePhone(phone)
  const ws = wsClients.get(normalizedPhone)
  if (ws && ws.readyState === WebSocket.OPEN) {
    const message = JSON.stringify({ type, ...data, timestamp: Date.now() })
    ws.send(message)
    log(`📤 WebSocket sent to ${normalizedPhone}: ${type}`)
  }
}

// Set up event callbacks on session manager
sessionManager.onPairingCode = (phone: string, code: string) => {
  sendToClient(phone, 'pairing_code', { code })
}

sessionManager.onConnected = (phone: string) => {
  sendToClient(phone, 'connected', {})
}

sessionManager.onError = (phone: string, error: string) => {
  sendToClient(phone, 'error', { error })
}

// CORS
app.use('*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST', 'DELETE'],
  allowHeaders: ['Content-Type'],
}))

// Request logging
app.use('*', async (c, next) => {
  const start = Date.now()
  await next()
  const ms = Date.now() - start
  log(`${c.req.method} ${c.req.path} - ${c.res.status} (${ms}ms)`)
})

// ============================================
// Health & Stats
// ============================================

app.get('/health', (c) => {
  const stats = sessionManager.getStats()
  return c.json({
    status: 'ok',
    ...stats,
    wsClients: wsClients.size,
    timestamp: Date.now()
  })
})

app.get('/stats', (c) => {
  return c.json(sessionManager.getStats())
})

// ============================================
// Session Management
// ============================================

// List all sessions
app.get('/sessions', (c) => {
  return c.json(sessionManager.getAllSessions())
})

// Get session status
app.get('/session/:userId/status', (c) => {
  const userId = c.req.param('userId')
  const status = sessionManager.getSessionStatus(userId)

  if (!status) {
    return c.json({ error: 'Session not found' }, 404)
  }

  return c.json(status)
})

// Request pairing code for a user
app.post('/session/:userId/pair', async (c) => {
  const userId = c.req.param('userId')
  const body = await c.req.json()
  const phoneNumber = body.phoneNumber

  if (!phoneNumber) {
    return c.json({ error: 'phoneNumber required' }, 400)
  }

  log(`Pairing request for user: ${userId}`)

  try {
    const code = await sessionManager.requestPairingCode(userId, phoneNumber)
    return c.json({
      success: true,
      userId,
      code,
      message: 'Enter this code in WhatsApp > Linked Devices > Link a Device'
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    log(`Pairing failed for ${userId}: ${msg}`)
    return c.json({ success: false, error: msg }, 500)
  }
})

// Disconnect/destroy a session
app.delete('/session/:userId', async (c) => {
  const userId = c.req.param('userId')
  log(`Destroying session: ${userId}`)
  await sessionManager.destroySession(userId)
  return c.json({ success: true, message: 'Session destroyed' })
})

// ============================================
// WhatsApp Contacts
// ============================================

// Get WhatsApp contacts with optional business detection
// Query params: limit (default 500), offset (default 0), detectBusiness (default true)
app.get('/session/:userId/contacts', async (c) => {
  const userId = c.req.param('userId')
  const limit = parseInt(c.req.query('limit') || '500')
  const offset = parseInt(c.req.query('offset') || '0')
  const detectBusiness = c.req.query('detectBusiness') !== 'false'

  const status = sessionManager.getSessionStatus(userId)
  if (!status || !status.connected) {
    return c.json({ success: false, error: 'Session not connected' }, 400)
  }

  const totalCount = sessionManager.getContactsCount(userId)
  log(`Contacts request for user: ${userId} (limit=${limit}, offset=${offset}, total=${totalCount})`)

  try {
    const contacts = await sessionManager.getContacts(userId, limit, offset, detectBusiness)
    const businessCount = contacts.filter(c => c.isBusiness).length
    const personalCount = contacts.length - businessCount

    return c.json({
      success: true,
      userId,
      total: totalCount,
      returned: contacts.length,
      offset,
      limit,
      businessCount,
      personalCount,
      contacts
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    log(`Contacts fetch failed for ${userId}: ${msg}`)
    return c.json({ success: false, error: msg }, 500)
  }
})

// ============================================
// WhatsApp Number Checking
// ============================================

// Check numbers using a specific user's session
app.post('/session/:userId/check', async (c) => {
  const userId = c.req.param('userId')
  const body = await c.req.json()
  const numbers = body.numbers

  if (!numbers || !Array.isArray(numbers)) {
    return c.json({ success: false, error: 'numbers array required' }, 400)
  }

  const status = sessionManager.getSessionStatus(userId)
  if (!status || !status.connected) {
    return c.json({ success: false, error: 'Session not connected' }, 400)
  }

  log(`Check request for user ${userId}: ${numbers.length} numbers`)

  try {
    const results = await sessionManager.checkNumbers(userId, numbers)
    const whatsappCount = results.filter(r => r.hasWhatsApp).length

    return c.json({
      success: true,
      userId,
      results,
      checked: results.length,
      withWhatsApp: whatsappCount
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    return c.json({ success: false, error: msg }, 500)
  }
})

// ============================================
// Legacy endpoints (backward compatible)
// Uses "default" as userId
// ============================================

app.get('/session/status', (c) => {
  const status = sessionManager.getSessionStatus('default')
  return c.json(status || { connected: false })
})

app.post('/session/pair', async (c) => {
  const body = await c.req.json()
  const phoneNumber = body.phoneNumber

  if (!phoneNumber) {
    return c.json({ error: 'phoneNumber required' }, 400)
  }

  try {
    const code = await sessionManager.requestPairingCode('default', phoneNumber)
    return c.json({
      success: true,
      code,
      message: 'Enter this code in WhatsApp > Linked Devices > Link a Device'
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    return c.json({ success: false, error: msg }, 500)
  }
})

app.post('/session/disconnect', async (c) => {
  await sessionManager.destroySession('default')
  return c.json({ success: true, message: 'Disconnected' })
})

app.post('/check', async (c) => {
  const body = await c.req.json()
  const numbers = body.numbers

  if (!numbers || !Array.isArray(numbers)) {
    return c.json({ success: false, error: 'numbers array required' }, 400)
  }

  const status = sessionManager.getSessionStatus('default')
  if (!status || !status.connected) {
    return c.json({ success: false, error: 'WhatsApp not connected' }, 400)
  }

  try {
    const results = await sessionManager.checkNumbers('default', numbers)
    return c.json({
      success: true,
      results,
      checked: results.length,
      withWhatsApp: results.filter(r => r.hasWhatsApp).length
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    return c.json({ success: false, error: msg }, 500)
  }
})

// ============================================
// Server Setup
// ============================================

const PORT = Number(process.env.PORT || 3456)

console.log('🚀 Starting WhatsApp Detector Service (Multi-Session)...')

const server = createServer(async (req: IncomingMessage, res: ServerResponse) => {
  const url = new URL(req.url || '/', `http://${req.headers.host || 'localhost'}`)

  let body: string | undefined
  if (req.method !== 'GET' && req.method !== 'HEAD') {
    body = await new Promise<string>((resolve) => {
      let data = ''
      req.on('data', (chunk: Buffer) => { data += chunk.toString() })
      req.on('end', () => resolve(data))
    })
  }

  const request = new Request(url.toString(), {
    method: req.method || 'GET',
    headers: Object.entries(req.headers).reduce((acc, [k, v]) => {
      if (v) acc[k] = Array.isArray(v) ? v.join(',') : v
      return acc
    }, {} as Record<string, string>),
    body: body || undefined
  })

  try {
    const response = await app.fetch(request)
    res.statusCode = response.status
    response.headers.forEach((value: string, key: string) => {
      res.setHeader(key, value)
    })
    const text = await response.text()
    res.end(text)
  } catch (err) {
    console.error('Request error:', err)
    res.statusCode = 500
    res.end('Internal Server Error')
  }
})

// ============================================
// WebSocket Server for Real-Time Pairing
// ============================================

const wss = new WebSocketServer({ server, path: '/ws/pairing' })

wss.on('connection', (ws) => {
  log('🔌 WebSocket client connected')

  ws.on('message', (data) => {
    try {
      const msg = JSON.parse(data.toString())
      log(`📩 WebSocket message: ${msg.type}`)

      if (msg.type === 'start_pairing' && msg.phoneNumber) {
        const phone = normalizePhone(msg.phoneNumber)
        // Remove any existing connection for this phone
        const existing = wsClients.get(phone)
        if (existing && existing !== ws) {
          existing.close()
        }
        wsClients.set(phone, ws)
        log(`📱 WebSocket registered for phone: ${phone}`)

        // Send acknowledgment
        ws.send(JSON.stringify({
          type: 'session_created',
          phoneNumber: phone,
          timestamp: Date.now()
        }))
      }
    } catch (err) {
      log(`❌ WebSocket message parse error: ${err}`)
    }
  })

  ws.on('close', () => {
    log('🔌 WebSocket client disconnected')
    // Remove from clients map
    for (const [phone, client] of wsClients.entries()) {
      if (client === ws) {
        wsClients.delete(phone)
        log(`📱 WebSocket unregistered for phone: ${phone}`)
        break
      }
    }
  })

  ws.on('error', (err) => {
    log(`❌ WebSocket error: ${err}`)
  })
})

log('✅ WebSocket server initialized at /ws/pairing')

// Graceful shutdown
process.on('SIGTERM', async () => {
  console.log('Received SIGTERM, shutting down...')
  await sessionManager.shutdown()
  wss.close()
  process.exit(0)
})

process.on('SIGINT', async () => {
  console.log('Received SIGINT, shutting down...')
  await sessionManager.shutdown()
  wss.close()
  process.exit(0)
})

server.listen(PORT, () => {
  console.log(`✅ Server running on http://localhost:${PORT}`)
  console.log(`🔌 WebSocket at ws://localhost:${PORT}/ws/pairing`)
  console.log(`📱 Multi-Session Endpoints:`)
  console.log(`   GET  /health`)
  console.log(`   GET  /sessions`)
  console.log(`   GET  /session/:userId/status`)
  console.log(`   POST /session/:userId/pair`)
  console.log(`   GET  /session/:userId/contacts`)
  console.log(`   POST /session/:userId/check`)
  console.log(`   DELETE /session/:userId`)
  console.log(`   WS   /ws/pairing  <- Real-time pairing notifications`)
  console.log(``)
  console.log(`📱 Legacy Endpoints (uses "default" user):`)
  console.log(`   GET  /session/status`)
  console.log(`   POST /session/pair`)
  console.log(`   POST /check`)
})
