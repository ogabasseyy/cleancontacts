import { serve } from '@hono/node-server'
import { Hono } from 'hono'
import { cors } from 'hono/cors'
import { secureHeaders } from 'hono/secure-headers'
import { rateLimiter } from 'hono-rate-limiter'
import { WebSocketServer } from 'ws'
import { createServer } from 'http'
import { SessionManager } from './session-manager.js'
import { logger, maskPII } from './src/logger.js'
import { envSchema, pairingRequestSchema, checkNumbersSchema } from './src/schemas.js'
import { z } from 'zod'

// Validate environment variables
const env = envSchema.parse(process.env)

const app = new Hono()
const sessionManager = new SessionManager()

// Web server setup
const PORT = env.PORT

// 2026 Best Practice: Single server instance for both HTTP and WebSocket
// Using a simple adapter to pipe Node requests to Hono without Hono's auto-listen
const server = createServer(async (req, res) => {
    try {
        const url = new URL(req.url || '/', `http://${req.headers.host}`)
        const request = new Request(url.toString(), {
            method: req.method,
            headers: req.headers as HeadersInit,
            body: req.method !== 'GET' && req.method !== 'HEAD' ? (req as any) : null,
            duplex: 'half' // Required for Node.js fetch with streaming body
        } as any)
        const response = await app.fetch(request)
        res.statusCode = response.status
        response.headers.forEach((value, key) => res.setHeader(key, value))
        const body = response.body
        if (body) {
            const reader = body.getReader()
            while (true) {
                const { done, value } = await reader.read()
                if (done) break
                res.write(value)
            }
        }
        res.end()
    } catch (err) {
        logger.error(`❌ Request error: ${err}`)
        res.statusCode = 500
        res.end('Internal Server Error')
    }
})

// Store WebSocket clients for live updates
const wsClients = new Map<string, any>()

const log = (msg: string) => logger.info(msg)

// 2026 Security: Restricted CORS
app.use('*', cors({
    origin: env.CORS_ORIGIN === '*' ? '*' : env.CORS_ORIGIN.split(','),
    allowMethods: ['GET', 'POST', 'DELETE'],
    allowHeaders: ['Content-Type', 'X-API-Key'],
}))

// 2026 Security: Secure Headers (Helmet replacement)
app.use('*', secureHeaders())

// 2026 Security: Rate Limiting
const limiter = rateLimiter({
    windowMs: 15 * 60 * 1000,
    limit: 100,
    standardHeaders: 'draft-6',
    keyGenerator: (c) => c.req.header('x-forwarded-for') || c.req.header('remote-addr') || 'anonymous',
})
app.use('*', limiter)

// 2026 Security: API Key Authentication
const apiAuth = async (c: any, next: any) => {
    const apiKey = c.req.header('X-API-Key')
    if (!apiKey || apiKey !== env.API_KEY) {
        logger.warn(`Unauthorized access attempt from ${c.req.header('x-forwarded-for') || 'unknown'}`)
        return c.json({ error: 'Unauthorized' }, 401)
    }
    await next()
}

// Global Logger Middleware
app.use('*', async (c, next) => {
    const start = Date.now()
    await next()
    const ms = Date.now() - start
    logger.info({
        method: c.req.method,
        path: c.req.path,
        status: c.res.status,
        duration: `${ms}ms`
    })
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

// ============================================
// Multi-Session Routes
// ============================================

// List all sessions (Authenticated)
app.get('/sessions', apiAuth, (c) => {
    return c.json(sessionManager.getAllSessions())
})

// Get status
app.get('/session/:userId/status', async (c) => {
    const userId = c.req.param('userId')
    const status = sessionManager.getSessionStatus(userId)
    if (!status) {
        return c.json({ connected: false, error: 'Session not found' })
    }
    return c.json(status)
})

// Request pairing
app.post('/session/:userId/pair', apiAuth, async (c) => {
    const userId = c.req.param('userId')
    try {
        const body = await c.req.json()
        const { phoneNumber } = pairingRequestSchema.parse(body)

        logger.info(`Pairing request for user: ${userId}`)
        const code = await sessionManager.requestPairingCode(userId, phoneNumber)

        return c.json({
            success: true,
            userId,
            code,
            message: 'Enter this code in WhatsApp > Linked Devices > Link a Device'
        })
    } catch (error) {
        if (error instanceof z.ZodError) {
            return c.json({ error: 'Invalid input', issues: error.issues }, 400)
        }
        const msg = error instanceof Error ? error.message : 'Unknown error'
        logger.error(`Pairing failed for ${userId}: ${msg}`)
        return c.json({ success: false, error: msg }, 500)
    }
})

// Check numbers
app.post('/session/:userId/check', apiAuth, async (c) => {
    const userId = c.req.param('userId')
    try {
        const body = await c.req.json()
        const { numbers } = checkNumbersSchema.parse(body)

        const results = await sessionManager.checkNumbers(userId, numbers)
        return c.json({ success: true, results })
    } catch (error) {
        if (error instanceof z.ZodError) {
            return c.json({ error: 'Invalid input', issues: error.issues }, 400)
        }
        const msg = error instanceof Error ? error.message : 'Unknown error'
        return c.json({ success: false, error: msg }, 500)
    }
})

// Disconnect
app.delete('/session/:userId', apiAuth, async (c) => {
    const userId = c.req.param('userId')
    logger.info(`Destroying session: ${userId}`)
    await sessionManager.destroySession(userId)
    return c.json({ success: true, message: 'Session destroyed' })
})

// ============================================
// WebSocket Server for Real-Time Updates
// ============================================

const wss = new WebSocketServer({ server, path: '/ws/pairing' })

wss.on('connection', (ws) => {
    logger.info('🔌 WebSocket client connected')

    ws.on('message', (data) => {
        try {
            const msg = JSON.parse(data.toString())
            if (msg.type === 'start_pairing' && msg.phoneNumber) {
                const phone = msg.phoneNumber.replace(/[^0-9]/g, '')
                wsClients.set(phone, ws)
                logger.info(`📱 WebSocket registered for phone: ${maskPII(phone)}`)

                ws.send(JSON.stringify({
                    type: 'session_created',
                    phoneNumber: phone,
                    timestamp: Date.now()
                }))
            }
        } catch (err) {
            logger.error(`❌ WebSocket message parse error: ${err}`)
        }
    })

    ws.on('close', () => {
        for (const [phone, client] of wsClients.entries()) {
            if (client === ws) {
                wsClients.delete(phone)
                break
            }
        }
    })
})

// Helper to push updates to WS clients
sessionManager.onPairingCode = (phone, code) => {
    const client = wsClients.get(phone)
    if (client) {
        client.send(JSON.stringify({ type: 'pairing_code', code, timestamp: Date.now() }))
    }
}

sessionManager.onConnected = (phone) => {
    const client = wsClients.get(phone)
    if (client) {
        client.send(JSON.stringify({ type: 'connected', timestamp: Date.now() }))
    }
}

// Graceful shutdown
const shutdown = async () => {
    logger.info('Received shutdown signal, stopping server...')
    await sessionManager.shutdown()
    wss.close()
    server.close(() => {
        logger.info('HTTP server closed')
        process.exit(0)
    })
}

process.on('SIGTERM', shutdown)
process.on('SIGINT', shutdown)

server.listen(PORT, () => {
    logger.info(`✅ Multi-Session Server running on port ${PORT}`)
})
