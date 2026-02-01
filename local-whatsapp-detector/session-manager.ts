/**
 * Multi-Session WhatsApp Manager
 * Each user gets their own isolated WhatsApp session
 *
 * 2026 Best Practice: Background business detection with verifiedName optimization
 */
import makeWASocket, {
  DisconnectReason,
  useMultiFileAuthState,
  fetchLatestBaileysVersion,
  makeCacheableSignalKeyStore,
  WASocket,
  Browsers,
  Contact
} from 'baileys'
import { Boom } from '@hapi/boom'
import pino from 'pino'
import { SocksProxyAgent } from 'socks-proxy-agent'
import { HttpsProxyAgent } from 'https-proxy-agent'
import * as fs from 'fs'
import * as path from 'path'

const logger = pino({ level: 'silent' })

const PROXY_URL = process.env.PROXY_URL
const SESSIONS_DIR = process.env.SESSIONS_DIR || './sessions'
const MAX_SESSIONS = parseInt(process.env.MAX_SESSIONS || '100')

// Session timeout - disconnect idle sessions after 30 minutes
const SESSION_IDLE_TIMEOUT = 30 * 60 * 1000

export interface SessionStatus {
  userId: string
  connected: boolean
  phoneNumber?: string
  lastActivity?: number
  createdAt?: number
  contactsCount?: number
  businessDetectionProgress?: {
    done: boolean
    inProgress: boolean
    checked: number
    total: number
    businessCount: number
  }
}

export interface CheckResult {
  number: string
  hasWhatsApp: boolean
  jid?: string
}

export interface WhatsAppContact {
  jid: string
  phoneNumber: string
  name?: string
  pushName?: string
  isBusiness: boolean
  businessProfile?: BusinessProfile
}

export interface BusinessProfile {
  description?: string
  category?: string
  email?: string
  website?: string[]
  address?: string
}

interface Session {
  userId: string
  sock: WASocket | null
  status: SessionStatus
  authPath: string
  lastActivity: number
  createdAt: number
  contacts: Map<string, Contact>
  // 2026 Best Practice: Track business detection state
  businessFlags: Map<string, boolean>
  businessDetectionDone: boolean
  businessDetectionInProgress: boolean
  businessDetectionChecked: number
  pairingPromise?: {
    resolve: (code: string) => void
    reject: (error: Error) => void
  }
}

export class SessionManager {
  private sessions: Map<string, Session> = new Map()
  private cleanupInterval: NodeJS.Timeout | null = null

  // Event callbacks for WebSocket notifications
  onPairingCode?: (phone: string, code: string) => void
  onConnected?: (phone: string) => void
  onError?: (phone: string, error: string) => void
  // 2026 Best Practice: Real-time business detection progress via WebSocket
  onBusinessDetectionProgress?: (phone: string, progress: { checked: number; total: number; businessCount: number; done: boolean }) => void

  // Track phone number for each session (for callbacks)
  private sessionPhones: Map<string, string> = new Map()

  constructor() {
    // Ensure sessions directory exists
    if (!fs.existsSync(SESSIONS_DIR)) {
      fs.mkdirSync(SESSIONS_DIR, { recursive: true })
    }

    // Start cleanup interval
    this.cleanupInterval = setInterval(() => this.cleanupIdleSessions(), 60000)
    console.log('📦 SessionManager initialized')
    console.log('   Max sessions: ' + MAX_SESSIONS)
    console.log('   Sessions dir: ' + SESSIONS_DIR)
    if (PROXY_URL) {
      console.log('   Proxy: ' + PROXY_URL.replace(/:[^:@]+@/, ':****@'))
    }

    // Auto-reconnect persisted sessions on startup
    this.autoReconnectSessions()
  }

  /**
   * Auto-reconnect sessions that have persisted auth state.
   * Runs on startup to restore previous sessions.
   */
  private async autoReconnectSessions(): Promise<void> {
    try {
      const dirs = fs.readdirSync(SESSIONS_DIR)
      for (const userId of dirs) {
        const authPath = path.join(SESSIONS_DIR, userId)
        const stat = fs.statSync(authPath)
        if (stat.isDirectory()) {
          // Check if auth files exist
          const credsPath = path.join(authPath, 'creds.json')
          if (fs.existsSync(credsPath)) {
            console.log('🔄 Auto-reconnecting session: ' + userId)
            try {
              await this.connectSession(userId)
              // Load persisted contacts after reconnect
              await this.loadContacts(userId)
            } catch (err: any) {
              console.error('❌ Failed to auto-reconnect ' + userId + ':', err.message)
            }
          }
        }
      }
    } catch (err) {
      console.error('Error during auto-reconnect:', err)
    }
  }

  /**
   * Save contacts to disk for persistence across restarts.
   * 2026 Best Practice: Also saves business detection flags.
   */
  private async saveContacts(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (!session) return

    const contactsPath = path.join(session.authPath, 'contacts.json')
    const contactsArray = Array.from(session.contacts.entries())
    const businessFlagsArray = Array.from(session.businessFlags.entries())

    const saveData = {
      contacts: contactsArray,
      businessFlags: businessFlagsArray,
      businessDetectionDone: session.businessDetectionDone,
      businessDetectionChecked: session.businessDetectionChecked
    }

    try {
      fs.writeFileSync(contactsPath, JSON.stringify(saveData))
      console.log('💾 Saved ' + contactsArray.length + ' contacts, ' + businessFlagsArray.length + ' business flags for session ' + userId)
    } catch (err) {
      console.error('Error saving contacts for ' + userId + ':', err)
    }
  }

  /**
   * Load contacts from disk after reconnect.
   * 2026 Best Practice: Also loads business detection flags.
   */
  private async loadContacts(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (!session) return

    const contactsPath = path.join(session.authPath, 'contacts.json')

    try {
      if (fs.existsSync(contactsPath)) {
        const data = fs.readFileSync(contactsPath, 'utf8')
        const parsed = JSON.parse(data)

        // Handle both old format (array) and new format (object with businessFlags)
        if (Array.isArray(parsed)) {
          // Old format - just contacts array
          const contactsArray: [string, Contact][] = parsed
          session.contacts = new Map(contactsArray)
        } else {
          // New format with business flags
          if (parsed.contacts) {
            session.contacts = new Map(parsed.contacts)
          }
          if (parsed.businessFlags) {
            session.businessFlags = new Map(parsed.businessFlags)
          }
          if (parsed.businessDetectionDone !== undefined) {
            session.businessDetectionDone = parsed.businessDetectionDone
          }
          if (parsed.businessDetectionChecked !== undefined) {
            session.businessDetectionChecked = parsed.businessDetectionChecked
          }
        }

        session.status.contactsCount = session.contacts.size
        console.log('📂 Loaded ' + session.contacts.size + ' contacts, ' + session.businessFlags.size + ' business flags for session ' + userId)

        // If business detection was not completed, resume it
        if (!session.businessDetectionDone && session.contacts.size > 0) {
          console.log('🔄 Resuming background business detection for ' + userId)
          this.runBackgroundBusinessDetection(userId)
        }
      }
    } catch (err) {
      console.error('Error loading contacts for ' + userId + ':', err)
    }
  }

  /**
   * 2026 Best Practice: Run business detection in background after contacts sync.
   *
   * Optimization strategy:
   * 1. First pass: Mark contacts with verifiedName as business (instant, free)
   * 2. Second pass: Fetch getBusinessProfile() for remaining contacts (slow, batched)
   */
  private async runBackgroundBusinessDetection(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (!session || !session.sock || !session.status.connected) return
    if (session.businessDetectionInProgress) return

    session.businessDetectionInProgress = true
    console.log('🔍 Starting background business detection for ' + userId + ' (' + session.contacts.size + ' contacts)')

    try {
      const jidsToCheck: string[] = []
      let verifiedBusinessCount = 0

      // First pass: Mark contacts with verifiedName as business (instant)
      for (const [jid, contact] of session.contacts) {
        // Skip groups, broadcasts, and status
        if (jid.endsWith('@g.us') || jid.endsWith('@broadcast') || jid === 'status@broadcast') continue

        // Check if already flagged
        if (session.businessFlags.has(jid)) continue

        // 2026 Optimization: verifiedName is only set for verified business accounts
        if ((contact as any).verifiedName) {
          session.businessFlags.set(jid, true)
          verifiedBusinessCount++
        } else {
          jidsToCheck.push(jid)
        }
      }

      console.log('🏢 ' + userId + ': ' + verifiedBusinessCount + ' verified businesses detected instantly')
      console.log('🔍 ' + userId + ': ' + jidsToCheck.length + ' contacts need getBusinessProfile() check')

      // Save after first pass
      this.saveContacts(userId)

      // Second pass: Fetch business profiles for remaining contacts (batched)
      // 2026 Optimization: Increased from 20/500ms to 50/200ms for faster detection
      const BATCH_SIZE = 50
      const BATCH_DELAY = 200 // ms between batches to avoid rate limiting

      for (let i = 0; i < jidsToCheck.length; i += BATCH_SIZE) {
        // Check if session is still connected
        if (!session.sock || !session.status.connected) {
          console.log('⚠️ ' + userId + ': Session disconnected, pausing business detection')
          break
        }

        const batch = jidsToCheck.slice(i, i + BATCH_SIZE)
        const profilePromises = batch.map(async (jid) => {
          try {
            const profile = await session.sock!.getBusinessProfile(jid)
            return { jid, isBusiness: !!profile }
          } catch {
            return { jid, isBusiness: false }
          }
        })

        const results = await Promise.all(profilePromises)
        for (const { jid, isBusiness } of results) {
          session.businessFlags.set(jid, isBusiness)
        }

        session.businessDetectionChecked = i + batch.length
        const businessCount = Array.from(session.businessFlags.values()).filter(v => v).length

        // 2026 Best Practice: Send real-time progress via WebSocket
        const phone = this.sessionPhones.get(userId)
        if (phone && this.onBusinessDetectionProgress) {
          this.onBusinessDetectionProgress(phone, {
            checked: i + batch.length,
            total: jidsToCheck.length,
            businessCount,
            done: false
          })
        }

        // Log progress every 100 contacts
        if ((i + batch.length) % 100 === 0 || i + batch.length === jidsToCheck.length) {
          console.log('🔍 ' + userId + ': Checked ' + (i + batch.length) + '/' + jidsToCheck.length + ' contacts, ' + businessCount + ' businesses found')
        }

        // Save progress periodically (every 200 contacts)
        if ((i + batch.length) % 200 === 0) {
          this.saveContacts(userId)
        }

        // Delay between batches
        if (i + BATCH_SIZE < jidsToCheck.length) {
          await new Promise(r => setTimeout(r, BATCH_DELAY))
        }
      }

      session.businessDetectionDone = true
      session.businessDetectionInProgress = false

      const finalBusinessCount = Array.from(session.businessFlags.values()).filter(v => v).length
      console.log('✅ ' + userId + ': Business detection complete! ' + finalBusinessCount + ' businesses out of ' + session.contacts.size + ' contacts')

      // 2026 Best Practice: Send final progress via WebSocket
      const phone = this.sessionPhones.get(userId)
      if (phone && this.onBusinessDetectionProgress) {
        this.onBusinessDetectionProgress(phone, {
          checked: jidsToCheck.length,
          total: jidsToCheck.length,
          businessCount: finalBusinessCount,
          done: true
        })
      }

      // Final save
      this.saveContacts(userId)

    } catch (err: any) {
      console.error('❌ ' + userId + ': Business detection error:', err.message)
      session.businessDetectionInProgress = false
    }
  }

  private getProxyAgent(): any {
    if (!PROXY_URL) return undefined
    if (PROXY_URL.startsWith('socks')) {
      return new SocksProxyAgent(PROXY_URL)
    }
    return new HttpsProxyAgent(PROXY_URL)
  }

  private async cleanupIdleSessions(): Promise<void> {
    const now = Date.now()
    for (const [userId, session] of this.sessions) {
      const idleTime = now - session.lastActivity
      if (idleTime > SESSION_IDLE_TIMEOUT && !session.status.connected) {
        console.log('🧹 Cleaning up idle session: ' + userId)
        await this.destroySession(userId)
      }
    }
  }

  async getOrCreateSession(userId: string): Promise<Session> {
    // Check if session exists
    let session = this.sessions.get(userId)
    if (session) {
      session.lastActivity = Date.now()
      return session
    }

    // Check max sessions limit
    if (this.sessions.size >= MAX_SESSIONS) {
      throw new Error('Max sessions limit (' + MAX_SESSIONS + ') reached')
    }

    // Create new session
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
        contactsCount: 0
      },
      authPath,
      lastActivity: Date.now(),
      createdAt: Date.now(),
      contacts: new Map(),
      // 2026 Best Practice: Initialize business detection state
      businessFlags: new Map(),
      businessDetectionDone: false,
      businessDetectionInProgress: false,
      businessDetectionChecked: 0
    }

    this.sessions.set(userId, session)
    console.log('📱 Created session for user: ' + userId)
    return session
  }

  async connectSession(userId: string, pairingPhoneNumber?: string): Promise<void> {
    const session = await this.getOrCreateSession(userId)

    // Close existing socket if any
    if (session.sock) {
      session.sock.end(undefined)
      session.sock = null
    }

    const { state, saveCreds } = await useMultiFileAuthState(session.authPath)
    const { version } = await fetchLatestBaileysVersion()

    const needsPairing = pairingPhoneNumber || !state.creds.registered
    const browserConfig = needsPairing
      ? Browsers.macOS('Chrome')
      : Browsers.ubuntu('Chrome')

    console.log('🔌 Connecting session ' + userId + ' (pairing: ' + !!pairingPhoneNumber + ')')

    const agent = this.getProxyAgent()

    session.sock = makeWASocket({
      version,
      auth: {
        creds: state.creds,
        keys: makeCacheableSignalKeyStore(state.keys, logger)
      },
      printQRInTerminal: false,
      logger,
      browser: browserConfig,
      generateHighQualityLinkPreview: false,
      connectTimeoutMs: 120000,
      defaultQueryTimeoutMs: 120000,
      keepAliveIntervalMs: 25000,
      markOnlineOnConnect: false,
      syncFullHistory: true,
      agent
    })

    // Listen for contacts sync
    session.sock.ev.on('contacts.upsert', (contacts) => {
      console.log('📇 Session ' + userId + ' contacts.upsert: ' + contacts.length + ' contacts')
      for (const contact of contacts) {
        if (contact.id) {
          session.contacts.set(contact.id, contact)
        }
      }
      session.status.contactsCount = session.contacts.size
      this.saveContacts(userId)
    })

    session.sock.ev.on('contacts.update', (updates) => {
      for (const update of updates) {
        if (update.id) {
          const existing = session.contacts.get(update.id)
          if (existing) {
            session.contacts.set(update.id, { ...existing, ...update })
          }
        }
      }
    })

    // Baileys v7: messaging-history.set contains contacts during full sync
    session.sock.ev.on('messaging-history.set', (data: any) => {
      console.log('📇 Session ' + userId + ' messaging-history.set received')
      let hadContacts = false
      if (data.contacts && Array.isArray(data.contacts)) {
        console.log('📇 Session ' + userId + ' got ' + data.contacts.length + ' contacts from history sync')
        for (const contact of data.contacts) {
          if (contact.id) {
            session.contacts.set(contact.id, contact)
          }
        }
        session.status.contactsCount = session.contacts.size
        hadContacts = true
      }
      if (data.chats && Array.isArray(data.chats)) {
        console.log('📇 Session ' + userId + ' got ' + data.chats.length + ' chats from history sync')
        for (const chat of data.chats) {
          if (chat.id && !chat.id.endsWith('@g.us') && !chat.id.endsWith('@broadcast')) {
            if (!session.contacts.has(chat.id)) {
              session.contacts.set(chat.id, { id: chat.id, name: chat.name })
            }
          }
        }
        session.status.contactsCount = session.contacts.size
        hadContacts = true
      }
      if (hadContacts) {
        this.saveContacts(userId)
        // 2026 Best Practice: Start background business detection after contacts sync
        setTimeout(() => {
          this.runBackgroundBusinessDetection(userId)
        }, 2000)
      }
    })

    // Handle pairing code request
    if (needsPairing && pairingPhoneNumber) {
      this.sessionPhones.set(userId, pairingPhoneNumber)

      try {
        await new Promise(resolve => setTimeout(resolve, 1500))
        const code = await session.sock.requestPairingCode(pairingPhoneNumber)
        console.log('🔑 Session ' + userId + ' pairing code: ' + code)

        if (this.onPairingCode) {
          this.onPairingCode(pairingPhoneNumber, code)
        }

        if (session.pairingPromise) {
          session.pairingPromise.resolve(code)
          session.pairingPromise = undefined
        }
      } catch (error: any) {
        console.error('❌ Session ' + userId + ' pairing error:', error.message)

        if (this.onError) {
          this.onError(pairingPhoneNumber, error.message)
        }

        if (session.pairingPromise) {
          session.pairingPromise.reject(error)
          session.pairingPromise = undefined
        }
      }
    }

    // Connection event handlers
    session.sock.ev.on("connection.update", async (update) => {
      const { connection, lastDisconnect } = update

      if (connection === "close") {
        const reason = (lastDisconnect?.error as Boom)?.output?.statusCode
        console.log('❌ Session ' + userId + ' disconnected: ' + (DisconnectReason[reason] || reason))

        session.status.connected = false

        const shouldReconnect = reason !== DisconnectReason.loggedOut

        if (reason === DisconnectReason.loggedOut) {
          console.log('🗑️ Session ' + userId + ' logged out, clearing auth')
          await this.clearSessionAuth(userId)
        } else if (shouldReconnect) {
          console.log('🔄 Session ' + userId + ' reconnecting (reason: ' + (DisconnectReason[reason] || reason) + ')...')
          setTimeout(() => {
            this.connectSession(userId).catch(err => {
              console.error('❌ Session ' + userId + ' reconnect failed:', err.message)
            })
          }, 3000)
        }
      } else if (connection === "open") {
        console.log('✅ Session ' + userId + ' connected!')
        session.status.connected = true
        session.status.lastActivity = Date.now()
        session.status.phoneNumber = session.sock?.user?.id?.split(":")[0]

        const phone = this.sessionPhones.get(userId)
        if (phone && this.onConnected) {
          this.onConnected(phone)
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

    return new Promise((resolve, reject) => {
      session.pairingPromise = { resolve, reject }

      if (session.sock) {
        session.sock.end(undefined)
        session.sock = null
      }

      this.connectSession(userId, formattedNumber).catch(reject)

      setTimeout(() => {
        if (session.pairingPromise) {
          session.pairingPromise.reject(new Error('Pairing code request timed out'))
          session.pairingPromise = undefined
        }
      }, 30000)
    })
  }

  async checkNumbers(userId: string, numbers: string[]): Promise<CheckResult[]> {
    const session = this.sessions.get(userId)

    if (!session || !session.sock || !session.status.connected) {
      throw new Error('Session not connected')
    }

    session.lastActivity = Date.now()

    const results: CheckResult[] = []
    const formattedNumbers = numbers.map(n => {
      let clean = n.replace(/[^0-9+]/g, '')
      if (clean.startsWith('+')) clean = clean.slice(1)
      return clean
    })

    const waResults = await session.sock.onWhatsApp(...formattedNumbers)

    for (let i = 0; i < formattedNumbers.length; i++) {
      const original = numbers[i]
      const formatted = formattedNumbers[i]
      const waResult = waResults?.find(r =>
        r.jid?.startsWith(formatted) || r.jid?.includes(formatted)
      )
      results.push({
        number: original,
        hasWhatsApp: waResult?.exists === true,
        jid: waResult?.jid
      })
    }

    return results
  }

  /**
   * Get WhatsApp contacts with business detection from cache.
   * 2026 Best Practice: Uses cached business flags from background detection.
   */
  async getContacts(
    userId: string,
    limit: number = 500,
    offset: number = 0,
    detectBusiness: boolean = true
  ): Promise<WhatsAppContact[]> {
    const session = this.sessions.get(userId)

    if (!session || !session.sock || !session.status.connected) {
      throw new Error('Session not connected')
    }

    session.lastActivity = Date.now()

    const allContacts: WhatsAppContact[] = []

    for (const [jid, contact] of session.contacts) {
      if (jid.endsWith('@g.us') || jid.endsWith('@broadcast')) continue
      if (jid === 'status@broadcast') continue

      const phoneNumber = jid.split('@')[0]

      // 2026 Best Practice: Use cached business flag, fallback to verifiedName check
      let isBusiness = session.businessFlags.get(jid) ?? false
      if (!session.businessFlags.has(jid) && (contact as any).verifiedName) {
        isBusiness = true
      }

      allContacts.push({
        jid,
        phoneNumber: '+' + phoneNumber,
        name: contact.name,
        pushName: contact.notify,
        isBusiness
      })
    }

    const paginatedContacts = allContacts.slice(offset, offset + limit)

    const businessCount = paginatedContacts.filter(c => c.isBusiness).length
    console.log('📇 Session ' + userId + ': ' + paginatedContacts.length + ' contacts (of ' + allContacts.length + ' total), ' + businessCount + ' businesses (detection: ' + (session.businessDetectionDone ? 'complete' : 'in progress') + ')')

    return paginatedContacts
  }

  getContactsCount(userId: string): number {
    const session = this.sessions.get(userId)
    if (!session) return 0
    return session.contacts.size
  }

  getBusinessDetectionStatus(userId: string): { done: boolean; inProgress: boolean; checked: number; total: number; businessCount: number } | null {
    const session = this.sessions.get(userId)
    if (!session) return null

    const total = Array.from(session.contacts.keys()).filter(jid =>
      !jid.endsWith('@g.us') && !jid.endsWith('@broadcast') && jid !== 'status@broadcast'
    ).length

    return {
      done: session.businessDetectionDone,
      inProgress: session.businessDetectionInProgress,
      checked: session.businessDetectionChecked,
      total,
      businessCount: Array.from(session.businessFlags.values()).filter(v => v).length
    }
  }

  async clearSessionAuth(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (session) {
      try {
        fs.rmSync(session.authPath, { recursive: true, force: true })
        fs.mkdirSync(session.authPath, { recursive: true })
      } catch (err) {
        console.error('Error clearing auth for ' + userId + ':', err)
      }
    }
  }

  async disconnectSession(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (session) {
      if (session.sock) {
        try {
          session.sock.end(undefined)
        } catch (err) {}
        session.sock = null
      }
      session.status.connected = false
      this.sessions.delete(userId)
      console.log('🔌 Disconnected session: ' + userId + ' (auth preserved)')
    }
  }

  async destroySession(userId: string): Promise<void> {
    const session = this.sessions.get(userId)
    if (session) {
      if (session.sock) {
        try {
          await session.sock.logout()
        } catch (err) {}
        session.sock = null
      }
      await this.clearSessionAuth(userId)
      this.sessions.delete(userId)
      console.log('🗑️ Destroyed session: ' + userId + ' (logged out)')
    }
  }

  getSessionStatus(userId: string): SessionStatus | null {
    const session = this.sessions.get(userId)
    if (!session) return null

    const businessStatus = this.getBusinessDetectionStatus(userId)

    return {
      ...session.status,
      lastActivity: session.lastActivity,
      contactsCount: session.contacts.size,
      businessDetectionProgress: businessStatus ?? undefined
    }
  }

  getAllSessions(): SessionStatus[] {
    return Array.from(this.sessions.values()).map(s => ({
      ...s.status,
      lastActivity: s.lastActivity,
      contactsCount: s.contacts.size
    }))
  }

  getStats(): { active: number; connected: number; max: number } {
    const sessions = Array.from(this.sessions.values())
    return {
      active: sessions.length,
      connected: sessions.filter(s => s.status.connected).length,
      max: MAX_SESSIONS
    }
  }

  async shutdown(): Promise<void> {
    console.log('🛑 Shutting down SessionManager (preserving auth)...')
    if (this.cleanupInterval) {
      clearInterval(this.cleanupInterval)
    }
    for (const [userId] of this.sessions) {
      await this.disconnectSession(userId)
    }
    console.log('✅ All sessions disconnected, auth preserved for auto-reconnect')
  }
}
