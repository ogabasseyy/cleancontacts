/**
 * WhatsApp Manager - Baileys Wrapper with Proxy Support
 * 2026 Best Practice: Use residential proxy to avoid datacenter IP blocks
 */
import makeWASocket, {
  DisconnectReason,
  useMultiFileAuthState,
  fetchLatestBaileysVersion,
  makeCacheableSignalKeyStore,
  WASocket,
  Browsers
} from 'baileys'
import { Boom } from '@hapi/boom'
import pino from 'pino'
import { SocksProxyAgent } from 'socks-proxy-agent'
import { HttpsProxyAgent } from 'https-proxy-agent'
import type { WebSocketManager } from './websocket.js'

const logger = pino({ level: 'silent' })

// Proxy configuration from environment
const PROXY_URL = process.env.PROXY_URL // e.g., socks5://user:pass@host:port

export interface CheckResult {
  number: string
  hasWhatsApp: boolean
  jid?: string
}

export interface SessionStatus {
  connected: boolean
  phoneNumber?: string
  lastConnected?: number
}

export class WhatsAppManager {
  private sock: WASocket | null = null
  private status: SessionStatus = { connected: false }
  private authPath = './auth_sessions'
  private wsManager: WebSocketManager | null = null
  private currentPairingPhone: string | null = null
  private pendingPairingResolve: ((code: string) => void) | null = null
  private pendingPairingReject: ((error: Error) => void) | null = null

  setWebSocketManager(wsManager: WebSocketManager) {
    this.wsManager = wsManager
    console.log('🔗 WebSocket manager linked to WhatsAppManager')
  }

  async initialize(): Promise<void> {
    console.log('📱 Initializing WhatsApp connection...')
    if (PROXY_URL) {
      console.log('🌐 Proxy configured: ' + PROXY_URL.replace(/:[^:@]+@/, ':****@'))
    } else {
      console.log('⚠️ No proxy configured - using direct connection')
      console.log('   Set PROXY_URL env var for residential proxy (recommended)')
    }
    await this.connect()
  }

  private getProxyAgent(): any {
    if (!PROXY_URL) return undefined
    
    if (PROXY_URL.startsWith('socks')) {
      return new SocksProxyAgent(PROXY_URL)
    } else {
      return new HttpsProxyAgent(PROXY_URL)
    }
  }

  private async connect(pairingPhoneNumber?: string): Promise<void> {
    const { state, saveCreds } = await useMultiFileAuthState(this.authPath)
    const { version } = await fetchLatestBaileysVersion()

    console.log(`🌐 Using WhatsApp Web v${version.join('.')}`)

    const needsPairing = pairingPhoneNumber || !state.creds.registered
    const browserConfig = needsPairing 
      ? Browsers.macOS('Chrome')
      : Browsers.ubuntu('Chrome')

    console.log(`🖥️ Browser config: ${JSON.stringify(browserConfig)}`)

    const agent = this.getProxyAgent()
    if (agent) {
      console.log('🔒 Using proxy agent for connection')
    }

    this.sock = makeWASocket({
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
      markOnlineOnConnect: false,
      syncFullHistory: false,
      // Proxy support
      agent: agent
    })

    if (needsPairing && pairingPhoneNumber) {
      console.log(`📲 Requesting pairing code for ${pairingPhoneNumber}...`)
      try {
        await new Promise(resolve => setTimeout(resolve, 1500))
        const code = await this.sock.requestPairingCode(pairingPhoneNumber)
        console.log(`🔑 Pairing code generated: ${code}`)
        
        if (this.wsManager) {
          this.wsManager.notifyPairingCode(pairingPhoneNumber, code)
        }
        
        if (this.pendingPairingResolve) {
          this.pendingPairingResolve(code)
          this.pendingPairingResolve = null
          this.pendingPairingReject = null
        }
      } catch (error: any) {
        console.error('❌ Error requesting pairing code:', error)
        if (this.pendingPairingReject) {
          this.pendingPairingReject(error)
          this.pendingPairingResolve = null
          this.pendingPairingReject = null
        }
        if (this.wsManager && pairingPhoneNumber) {
          this.wsManager.notifyError(pairingPhoneNumber, error.message)
        }
      }
    }

    this.sock.ev.on('connection.update', async (update) => {
      const { connection, lastDisconnect } = update

      if (connection === 'close') {
        const reason = (lastDisconnect?.error as Boom)?.output?.statusCode
        const reasonMessage = (lastDisconnect?.error as Boom)?.message || 'Unknown error'
        console.log(`❌ Connection closed: ${DisconnectReason[reason] || reason} (${reasonMessage})`)
        
        this.status.connected = false

        if (reason !== DisconnectReason.loggedOut && !this.currentPairingPhone) {
          console.log('🔄 Reconnecting...')
          setTimeout(() => this.connect(), 5000)
        } else if (reason === DisconnectReason.loggedOut) {
          console.log('⚠️ Session logged out. Ready for new pairing.')
        }
          
      } else if (connection === 'open') {
        console.log('✅ WhatsApp connected!')
        this.status.connected = true
        this.status.lastConnected = Date.now()
        this.status.phoneNumber = this.sock?.user?.id?.split(':')[0]

        if (this.wsManager) {
          this.wsManager.notifyConnected(this.currentPairingPhone || undefined)
        }
        this.currentPairingPhone = null
      }
    })

    this.sock.ev.on('creds.update', saveCreds)
  }

  async requestPairingCode(phoneNumber: string): Promise<string> {
    const formattedNumber = phoneNumber.replace(/[^0-9]/g, '')
    
    if (formattedNumber.length < 10) {
      throw new Error('Invalid phone number format')
    }

    this.currentPairingPhone = formattedNumber

    return new Promise((resolve, reject) => {
      this.pendingPairingResolve = resolve
      this.pendingPairingReject = reject
      
      if (this.sock) {
        this.sock.end(undefined)
        this.sock = null
      }
      
      this.connect(formattedNumber).catch(reject)
      
      setTimeout(() => {
        if (this.pendingPairingReject) {
          this.pendingPairingReject(new Error('Pairing code request timed out'))
          this.pendingPairingResolve = null
          this.pendingPairingReject = null
        }
      }, 30000)
    })
  }

  async disconnect(): Promise<void> {
    if (this.sock) {
      try {
        await this.sock.logout()
      } catch (err) {}
      this.sock = null
      this.status = { connected: false }
      this.currentPairingPhone = null
      console.log('👋 Disconnected from WhatsApp')
    }
  }

  isConnected(): boolean {
    return this.status.connected
  }

  getStatus(): SessionStatus {
    return this.status
  }

  async checkNumbers(numbers: string[]): Promise<CheckResult[]> {
    if (!this.sock || !this.status.connected) {
      throw new Error('WhatsApp not connected')
    }

    const results: CheckResult[] = []
    const formattedNumbers = numbers.map(n => {
      let clean = n.replace(/[^0-9+]/g, '')
      if (clean.startsWith('+')) clean = clean.slice(1)
      return clean
    })

    try {
      const waResults = await this.sock.onWhatsApp(...formattedNumbers)
      
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
    } catch (error) {
      console.error('Error checking numbers:', error)
      throw error
    }

    return results
  }

  async checkNumbersBatch(
    numbers: string[], 
    batchSize: number = 50,
    delayMs: number = 1000
  ): Promise<CheckResult[]> {
    const results: CheckResult[] = []
    
    for (let i = 0; i < numbers.length; i += batchSize) {
      const batch = numbers.slice(i, i + batchSize)
      console.log(`📊 Checking batch ${Math.floor(i/batchSize) + 1}/${Math.ceil(numbers.length/batchSize)}...`)
      
      try {
        const batchResults = await this.checkNumbers(batch)
        results.push(...batchResults)
      } catch (err) {
        console.error('Batch error:', err)
        throw err
      }
      
      if (i + batchSize < numbers.length) {
        await new Promise(resolve => setTimeout(resolve, delayMs))
      }
    }
    
    return results
  }
}
