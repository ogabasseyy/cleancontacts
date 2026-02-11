import {
    makeWASocket,
    DisconnectReason,
    useMultiFileAuthState,
    fetchLatestBaileysVersion,
    Contact
} from "baileys"
import { Boom } from "@hapi/boom"
import path from "path"
import fs from "fs"
import { fileURLToPath } from 'url'
import { logger, maskPII } from './src/logger.js'

const __filename = fileURLToPath(import.meta.url)
const __dirname = path.dirname(__filename)

const SESSIONS_DIR = path.join(__dirname, 'sessions');
const MAX_SESSIONS = 50
const SESSION_IDLE_TIMEOUT = 24 * 60 * 60 * 1000 // 24 hours

export interface SessionStatus {
    userId: string
    connected: boolean
    phoneNumber?: string
    lastActivity: number
    createdAt: number
    contactsCount: number
    businessDetectionProgress?: {
        done: boolean
        inProgress: boolean
        checked: number
        total: number
        businessCount: number
    }
}

interface Session {
    userId: string
    sock: any | null
    status: SessionStatus
    authPath: string
    lastActivity: number
    contacts: Map<string, Contact>
    pairingPromise?: {
        resolve: (code: string) => void
        reject: (err: any) => void
    }
}

export class SessionManager {
    private sessions: Map<string, Session> = new Map()
    private sessionPhones: Map<string, string> = new Map() // userId -> phone
    private cleanupInterval: NodeJS.Timeout | null = null

    // Callbacks for live updates
    public onPairingCode?: (phone: string, code: string) => void
    public onConnected?: (phone: string) => void
    public onBusinessDetectionProgress?: (phone: string, progress: any) => void

    constructor() {
        if (!fs.existsSync(SESSIONS_DIR)) {
            fs.mkdirSync(SESSIONS_DIR, { recursive: true })
        }
        this.cleanupInterval = setInterval(() => this.cleanupIdleSessions(), 60 * 60 * 1000)
        logger.info('🚀 SessionManager initialized')
    }

    private async saveContacts(userId: string): Promise<void> {
        const session = this.sessions.get(userId)
        if (!session) return

        const contactsPath = path.join(session.authPath, 'contacts.json')
        try {
            const contactsArray = Array.from(session.contacts.entries())
            fs.writeFileSync(contactsPath, JSON.stringify(contactsArray), 'utf8')
            logger.info(`💾 Saved ${contactsArray.length} contacts for session ${userId}`)
        } catch (err) {
            logger.error(`Error saving contacts for ${userId}: ${err}`)
        }
    }

    private async loadContacts(userId: string): Promise<void> {
        const session = this.sessions.get(userId)
        if (!session) return

        const contactsPath = path.join(session.authPath, 'contacts.json')
        try {
            if (fs.existsSync(contactsPath)) {
                const data = fs.readFileSync(contactsPath, 'utf8')
                const parsed = JSON.parse(data)
                if (Array.isArray(parsed)) {
                    session.contacts = new Map(parsed)
                }
            }
        } catch (err) {
            logger.error(`Error loading contacts for ${userId}: ${err}`)
        }
    }

    private async cleanupIdleSessions() {
        const now = Date.now()
        for (const [userId, session] of this.sessions) {
            const idleTime = now - session.lastActivity
            if (idleTime > SESSION_IDLE_TIMEOUT && !session.status.connected) {
                logger.info(`扫 Cleaning up idle session: ${userId}`)
                await this.destroySession(userId)
            }
        }
    }

    async getOrCreateSession(userId: string): Promise<Session> {
        let session = this.sessions.get(userId)
        if (session) {
            session.lastActivity = Date.now()
            return session
        }

        if (this.sessions.size >= MAX_SESSIONS) {
            throw new Error(`Max sessions limit (${MAX_SESSIONS}) reached`)
        }

        const authPath = path.join(SESSIONS_DIR, userId)
        if (!fs.existsSync(authPath)) {
            fs.mkdirSync(authPath, { recursive: true })
        }

        session = {
            userId,
            sock: null,
            status: {
                userId,
                connected: false,
                createdAt: Date.now(),
                contactsCount: 0,
                lastActivity: Date.now()
            },
            authPath,
            lastActivity: Date.now(),
            contacts: new Map()
        }

        this.sessions.set(userId, session)
        await this.loadContacts(userId)
        return session
    }

    async connectSession(userId: string, phoneNumber?: string): Promise<void> {
        const session = await this.getOrCreateSession(userId)
        if (session.status.connected && session.sock) return

        const { state, saveCreds } = await useMultiFileAuthState(session.authPath)
        const { version } = await fetchLatestBaileysVersion()

        session.sock = makeWASocket({
            version,
            printQRInTerminal: false,
            auth: state,
            browser: ["Awoof", "Chrome", "1.0.0"],
        })

        if (phoneNumber && !state.creds.registered) {
            setTimeout(async () => {
                try {
                    const code = await session.sock.requestPairingCode(phoneNumber)
                    logger.info(`🔑 Pairing code generated for ${maskPII(phoneNumber)}`)
                    if (session.pairingPromise) {
                        session.pairingPromise.resolve(code)
                        session.pairingPromise = undefined
                    }
                    if (this.onPairingCode) {
                        this.onPairingCode(phoneNumber, code)
                    }
                } catch (err) {
                    logger.error(`Failed to request pairing code for ${userId}: ${err}`)
                    if (session.pairingPromise) {
                        session.pairingPromise.reject(err)
                        session.pairingPromise = undefined
                    }
                }
            }, 3000)
        }

        session.sock.ev.on("connection.update", async (update: any) => {
            const { connection, lastDisconnect } = update
            if (connection === "close") {
                const shouldReconnect = (lastDisconnect?.error as Boom)?.output?.statusCode !== DisconnectReason.loggedOut
                session.status.connected = false
                logger.warn(`🔌 Connection closed for ${userId}. Reconnect: ${shouldReconnect}`)
                if (shouldReconnect) {
                    this.connectSession(userId)
                } else {
                    this.destroySession(userId)
                }
            } else if (connection === "open") {
                logger.info(`✅ Session ${userId} connected!`)
                session.status.connected = true
                session.lastActivity = Date.now()
                session.status.phoneNumber = session.sock?.user?.id?.split(":")[0]
                if (session.status.phoneNumber && this.onConnected) {
                    this.onConnected(session.status.phoneNumber)
                }
            }
        })

        session.sock.ev.on("creds.update", saveCreds)
    }

    async requestPairingCode(userId: string, phoneNumber: string): Promise<string> {
        const formattedNumber = phoneNumber.replace(/[^0-9]/g, '')
        if (formattedNumber.length < 10) {
            throw new Error('Invalid phone number format')
        }

        const session = await this.getOrCreateSession(userId)
        session.lastActivity = Date.now()
        this.sessionPhones.set(userId, formattedNumber)

        return new Promise((resolve, reject) => {
            session.pairingPromise = { resolve, reject }
            this.connectSession(userId, formattedNumber)
        })
    }

    async checkNumbers(userId: string, numbers: string[]): Promise<any[]> {
        const session = this.sessions.get(userId)
        if (!session || !session.sock || !session.status.connected) {
            throw new Error('Session not connected')
        }
        session.lastActivity = Date.now()

        const results = []
        for (const num of numbers) {
            const formatted = num.replace(/[^0-9]/g, '')
            try {
                const [result] = await session.sock.onWhatsApp(formatted)
                results.push({
                    number: num,
                    exists: !!result,
                    jid: result?.jid
                })
            } catch (err) {
                results.push({ number: num, exists: false, error: 'Check failed' })
            }
        }
        return results
    }

    async destroySession(userId: string): Promise<void> {
        const session = this.sessions.get(userId)
        if (session) {
            if (session.sock) {
                try {
                    await session.sock.logout()
                } catch (err) { }
                session.sock = null
            }
            const authPath = session.authPath
            if (fs.existsSync(authPath)) {
                fs.rmSync(authPath, { recursive: true, force: true })
            }
            this.sessions.delete(userId)
            logger.info(`🗑️ Destroyed session: ${userId}`)
        }
    }

    async shutdown(): Promise<void> {
        logger.info('🛑 Shutting down SessionManager...')
        if (this.cleanupInterval) clearInterval(this.cleanupInterval)
        for (const [userId] of this.sessions) {
            const session = this.sessions.get(userId)
            if (session?.sock) {
                session.sock.end(undefined)
            }
        }
    }

    getAllSessions(): SessionStatus[] {
        return Array.from(this.sessions.values()).map(s => ({
            ...s.status,
            lastActivity: s.lastActivity,
            contactsCount: s.contacts.size
        }))
    }

    getSessionStatus(userId: string): SessionStatus | null {
        const session = this.sessions.get(userId)
        return session ? { ...session.status, lastActivity: session.lastActivity, contactsCount: session.contacts.size } : null
    }

    getStats() {
        return {
            active: this.sessions.size,
            connected: Array.from(this.sessions.values()).filter(s => s.status.connected).length,
            max: MAX_SESSIONS
        }
    }
}
