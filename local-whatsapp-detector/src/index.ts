/**
 * WhatsApp Detector Service with WebSocket Support
 * 2026 Best Practices: Hono + Baileys + WebSocket + TypeScript ESM
 */
import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { createServer, IncomingMessage, ServerResponse } from 'http'

import { WhatsAppManager } from './whatsapp.js'
import { WebSocketManager } from './websocket.js'

const app = new Hono()
const waManager = new WhatsAppManager()
let wsManager: WebSocketManager | null = null

// PRIVACY: Custom logger
const privacyLogger = (message: string, count?: number) => {
  const timestamp = new Date().toISOString()
  if (count !== undefined) {
    console.log(`[${timestamp}] ${message}: ${count} items`)
  } else {
    console.log(`[${timestamp}] ${message}`)
  }
}

// CORS
app.use('*', cors({
  origin: '*',
  allowMethods: ['GET', 'POST'],
  allowHeaders: ['Content-Type'],
}))

// Request logging
app.use('*', async (c, next) => {
  const start = Date.now()
  await next()
  const ms = Date.now() - start
  privacyLogger(`${c.req.method} ${c.req.path} - ${c.res.status} (${ms}ms)`)
})

// Health check - respects pairing-in-progress
app.get('/health', (c) => {
  const isPairing = wsManager?.isPairingInProgress() ?? false
  const isConnected = waManager.isConnected()
  
  if (isPairing) {
    return c.json({ 
      status: 'ok', 
      pairing: true,
      sessions: wsManager?.getActiveSessionCount() ?? 0,
      timestamp: Date.now()
    })
  }
  
  return c.json({ 
    status: isConnected ? 'ok' : 'degraded', 
    connected: isConnected,
    timestamp: Date.now()
  })
})

// Session status
app.get('/session/status', (c) => {
  return c.json(waManager.getStatus())
})

// Request pairing code
app.post('/session/pair', async (c) => {
  const body = await c.req.json()
  const phoneNumber = body.phoneNumber
  
  if (!phoneNumber) {
    return c.json({ error: 'phoneNumber required' }, 400)
  }
  
  privacyLogger('Pairing code requested')
  
  try {
    const code = await waManager.requestPairingCode(phoneNumber)
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

// Disconnect
app.post('/session/disconnect', async (c) => {
  privacyLogger('Session disconnect requested')
  await waManager.disconnect()
  return c.json({ success: true, message: 'Disconnected' })
})

// Check numbers
app.post('/check', async (c) => {
  const body = await c.req.json()
  const numbers = body.numbers
  
  if (!numbers || !Array.isArray(numbers)) {
    return c.json({ success: false, error: 'numbers array required' }, 400)
  }
  
  if (!waManager.isConnected()) {
    return c.json({ success: false, error: 'WhatsApp not connected' }, 400)
  }
  
  privacyLogger('WhatsApp check requested', numbers.length)
  
  try {
    const results = await waManager.checkNumbers(numbers)
    const whatsappCount = results.filter(r => r.hasWhatsApp).length
    
    return c.json({ 
      success: true, 
      results,
      checked: results.length,
      withWhatsApp: whatsappCount
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    return c.json({ success: false, error: msg }, 500)
  }
})

// Batch check
app.post('/check/batch', async (c) => {
  const body = await c.req.json()
  const { numbers, batchSize = 50 } = body
  
  if (!numbers || !Array.isArray(numbers)) {
    return c.json({ success: false, error: 'numbers array required' }, 400)
  }
  
  if (!waManager.isConnected()) {
    return c.json({ success: false, error: 'WhatsApp not connected' }, 400)
  }
  
  privacyLogger('Batch check requested', numbers.length)
  
  try {
    const results = await waManager.checkNumbersBatch(numbers, batchSize)
    const whatsappCount = results.filter(r => r.hasWhatsApp).length
    
    return c.json({
      success: true,
      total: numbers.length,
      checked: results.length,
      whatsappCount,
      results
    })
  } catch (error: unknown) {
    const msg = error instanceof Error ? error.message : 'Unknown error'
    return c.json({ success: false, error: msg }, 500)
  }
})

// Start server
const PORT = Number(process.env.PORT || 3456)

console.log('🚀 Starting WhatsApp Detector Service...')

// Create HTTP server
const server = createServer(async (req: IncomingMessage, res: ServerResponse) => {
  // Skip WebSocket upgrade requests
  if (req.headers.upgrade === 'websocket') {
    return
  }

  // Convert Node request to fetch Request
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

waManager.initialize().then(() => {
  // Initialize WebSocket manager
  wsManager = new WebSocketManager(server)
  waManager.setWebSocketManager(wsManager)

  server.listen(PORT, () => {
    console.log(`✅ Server running on http://localhost:${PORT}`)
    console.log(`🔌 WebSocket at ws://localhost:${PORT}/ws/pairing`)
    console.log(`📱 Endpoints:`)
    console.log(`   GET  /health`)
    console.log(`   GET  /session/status`)
    console.log(`   POST /session/pair`)
    console.log(`   POST /session/disconnect`)
    console.log(`   POST /check`)
    console.log(`   POST /check/batch`)
    console.log(`   WS   /ws/pairing`)
  })
})
