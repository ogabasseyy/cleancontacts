/**
 * WhatsApp Detector Service with WebSocket Support
 * 2026 Best Practices: Hono + Baileys + WebSocket + TypeScript ESM
 */
import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { secureHeaders } from 'hono/secure-headers'
import { rateLimiter } from 'hono-rate-limiter'
import { createServer, IncomingMessage, ServerResponse } from 'http'
import { z } from 'zod'

import { WhatsAppManager } from './whatsapp.js'
import { WebSocketManager } from './websocket.js'
import { logger, maskPII } from './logger.js'
import { envSchema, pairingRequestSchema, checkNumbersSchema, batchCheckSchema } from './schemas.js'

// 2026 Best Practice: Environment Validation
const env = envSchema.parse(process.env)

const app = new Hono()
const waManager = new WhatsAppManager()
let wsManager: WebSocketManager | null = null

// 2026 Best Practice: Secure Headers (Helmet equivalent)
app.use('*', secureHeaders())

// 2026 Best Practice: Rate Limiting
const limiter = rateLimiter({
  windowMs: 15 * 60 * 1000,
  limit: 100,
  standardHeaders: 'draft-7',
  keyGenerator: (c) => c.req.header('x-forwarded-for') || 'anon',
})
app.use('/session/*', limiter)
app.use('/check/*', limiter)

// CORS - 2026 Best Practice: Restrict to known origins
app.use('*', cors({
  origin: env.CORS_ORIGIN === '*' ? '*' : env.CORS_ORIGIN.split(','),
  allowMethods: ['GET', 'POST'],
  allowHeaders: ['Content-Type', 'X-API-Key'],
}))

// 2026 Best Practice: API Key Defense in Depth
const apiAuth = async (c: any, next: any) => {
  const apiKey = c.req.header('X-API-Key')
  if (env.API_KEY && apiKey !== env.API_KEY) {
    logger.warn({ path: c.req.path }, 'Unauthorized API access attempt')
    return c.json({ error: 'Unauthorized' }, 401)
  }
  await next()
}

// Request logging - Structured
app.use('*', async (c, next) => {
  const start = Date.now()
  await next()
  const ms = Date.now() - start
  logger.info({
    method: c.req.method,
    path: c.req.path,
    status: c.res.status,
    duration: `${ms}ms`
  }, 'Request processed')
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

// Request pairing code - 2026 Best Practice: Zod Validation
app.post('/session/pair', apiAuth, async (c) => {
  try {
    const body = await c.req.json()
    const { phoneNumber } = pairingRequestSchema.parse(body)

    logger.info({ phoneNumber: maskPII(phoneNumber) }, 'Pairing code requested')

    const code = await waManager.requestPairingCode(phoneNumber)
    return c.json({
      success: true,
      code,
      message: 'Enter this code in WhatsApp > Linked Devices > Link a Device'
    })
  } catch (error: unknown) {
    if (error instanceof z.ZodError) {
      return c.json({ success: false, error: 'Invalid phone number format' }, 400)
    }
    const msg = error instanceof Error ? error.message : 'Unknown error'
    logger.error({ error: msg }, 'Pairing failed')
    return c.json({ success: false, error: msg }, 500)
  }
})

// Disconnect
app.post('/session/disconnect', apiAuth, async (c) => {
  logger.info('Session disconnect requested')
  await waManager.disconnect()
  return c.json({ success: true, message: 'Disconnected' })
})

// Check numbers - 2026 Best Practice: Zod Validation
app.post('/check', apiAuth, async (c) => {
  try {
    const body = await c.req.json()
    const { numbers } = checkNumbersSchema.parse(body)

    if (!waManager.isConnected()) {
      return c.json({ success: false, error: 'WhatsApp not connected' }, 400)
    }

    logger.info({ count: numbers.length }, 'WhatsApp check requested')
    const results = await waManager.checkNumbers(numbers)

    return c.json({
      success: true,
      results,
      checked: results.length,
      withWhatsApp: results.filter(r => r.hasWhatsApp).length
    })
  } catch (error: unknown) {
    if (error instanceof z.ZodError) {
      return c.json({ success: false, error: error.issues }, 400)
    }
    const msg = error instanceof Error ? error.message : 'Unknown error'
    logger.error({ error: msg }, 'Check failed')
    return c.json({ success: false, error: msg }, 500)
  }
})

// Batch check
app.post('/check/batch', apiAuth, async (c) => {
  try {
    const body = await c.req.json()
    const { numbers, batchSize } = batchCheckSchema.parse(body)

    if (!waManager.isConnected()) {
      return c.json({ success: false, error: 'WhatsApp not connected' }, 400)
    }

    logger.info({ count: numbers.length, batchSize }, 'Batch check requested')
    const results = await waManager.checkNumbersBatch(numbers, batchSize)

    return c.json({
      success: true,
      total: numbers.length,
      checked: results.length,
      whatsappCount: results.filter(r => r.hasWhatsApp).length,
      results
    })
  } catch (error: unknown) {
    if (error instanceof z.ZodError) {
      return c.json({ success: false, error: error.issues }, 400)
    }
    const msg = error instanceof Error ? error.message : 'Unknown error'
    logger.error({ error: msg }, 'Batch check failed')
    return c.json({ success: false, error: msg }, 500)
  }
})

// Start server
const PORT = env.PORT

logger.info('🚀 Starting WhatsApp Detector Service...')

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
    logger.error({ error: err }, 'Request error')
    res.statusCode = 500
    res.end('Internal Server Error')
  }
})

// 2026 Best Practice: Graceful Shutdown
const shutdown = async (signal: string) => {
  logger.info({ signal }, 'Shutting down gracefully...')
  if (waManager.isConnected()) {
    await waManager.disconnect()
  }
  server.close(() => {
    logger.info('Server closed')
    process.exit(0)
  })

  // Force exit if shutdown takes too long
  setTimeout(() => {
    logger.error('Could not close connections in time, forceful exit')
    process.exit(1)
  }, 10000)
}

process.on('SIGTERM', () => shutdown('SIGTERM'))
process.on('SIGINT', () => shutdown('SIGINT'))

waManager.initialize().then(() => {
  // Initialize WebSocket manager
  wsManager = new WebSocketManager(server)
  waManager.setWebSocketManager(wsManager)

  server.listen(PORT, () => {
    logger.info({ port: PORT }, '✅ Server running')
    logger.info(`🔌 WebSocket at ws://localhost:${PORT}/ws/pairing`)
  })
})
