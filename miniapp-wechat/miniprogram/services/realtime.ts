import { MiniAppLoginResponse, Overview, RealtimeDashboard, Task } from '../types/api'
import { getApiBaseUrl, isMockMode } from './config'
import { mockDashboard } from './mock'
import { handleSessionExpired } from './session'

export type RealtimeState = 'connecting' | 'connected' | 'disconnected' | 'unavailable' | 'sessionExpired'

export interface DashboardRealtimeUpdate {
  overview: Overview
  tasks?: Task[]
}

export interface DashboardRealtimeConnection {
  disconnect(): void
}

interface RealtimeCallbacks {
  onUpdate(update: DashboardRealtimeUpdate): void
  onState(state: RealtimeState): void
}

interface StompFrame {
  command: string
  headers: Record<string, string>
  body: string
}

const RECONNECT_DELAYS = [1000, 3000, 5000, 10000, 30000]
const SUBSCRIPTION_ID = 'mini-app-dashboard'

export function connectDashboardRealtime(callbacks: RealtimeCallbacks): DashboardRealtimeConnection {
  if (isMockMode()) return connectMockRealtime(callbacks)

  const token = wx.getStorageSync('mes_token') as string
  const user = (wx.getStorageSync('mes_user') || {}) as MiniAppLoginResponse
  const destination = getDestination(user)
  if (!token || !destination) {
    callbacks.onState('unavailable')
    return { disconnect() {} }
  }

  return new StompDashboardConnection(token, destination, callbacks)
}

function connectMockRealtime(callbacks: RealtimeCallbacks): DashboardRealtimeConnection {
  let stopped = false
  callbacks.onState('connected')

  const timer = setInterval(async () => {
    if (stopped) return
    try {
      callbacks.onUpdate(await mockDashboard())
    } catch (error) {
      const message = (error as Error).message || ''
      callbacks.onState(message.includes('登录已失效') ? 'sessionExpired' : 'disconnected')
    }
  }, 15000)

  return {
    disconnect() {
      stopped = true
      clearInterval(timer)
    }
  }
}

function getDestination(user: MiniAppLoginResponse): string {
  if (user.lineId) return `/topic/report/mini_app/realtime/line/${user.lineId}`
  if (user.workshopId) return `/topic/report/mini_app/realtime/workshop/${user.workshopId}`
  return ''
}

class StompDashboardConnection implements DashboardRealtimeConnection {
  private socket?: WechatMiniprogram.SocketTask
  private reconnectTimer = 0 as unknown as number
  private reconnectAttempt = 0
  private stopped = false
  private frameBuffer = ''

  constructor(
    private readonly token: string,
    private readonly destination: string,
    private readonly callbacks: RealtimeCallbacks
  ) {
    this.connect()
  }

  disconnect(): void {
    this.stopped = true
    clearTimeout(this.reconnectTimer)
    this.reconnectTimer = 0 as unknown as number
    if (!this.socket) return

    this.sendFrame('DISCONNECT', { receipt: 'disconnect-receipt' })
    this.socket.close({ code: 1000, reason: '页面离开' })
    this.socket = undefined
  }

  private connect(): void {
    if (this.stopped) return

    this.callbacks.onState('connecting')
    this.frameBuffer = ''
    const socket = wx.connectSocket({ url: getSocketUrl(), protocols: ['v12.stomp'] })
    this.socket = socket
    socket.onOpen(() => this.sendConnect())
    socket.onMessage(event => this.consumeMessage(event.data))
    socket.onError(() => this.handleDisconnected())
    socket.onClose(() => this.handleDisconnected())
  }

  private sendConnect(): void {
    this.sendFrame('CONNECT', {
      'accept-version': '1.2',
      'heart-beat': '0,0',
      Authorization: `Bearer ${this.token}`
    })
  }

  private subscribe(): void {
    this.sendFrame('SUBSCRIBE', {
      id: SUBSCRIPTION_ID,
      destination: this.destination,
      ack: 'auto'
    })
  }

  private sendFrame(command: string, headers: Record<string, string>, body = ''): void {
    if (!this.socket) return

    const headerText = Object.keys(headers).map(key => `${key}:${headers[key]}`).join('\n')
    this.socket.send({ data: `${command}\n${headerText}\n\n${body}\0` })
  }

  private consumeMessage(data: string | ArrayBuffer): void {
    if (typeof data !== 'string') return

    this.frameBuffer += data
    let frameEnd = this.frameBuffer.indexOf('\0')
    while (frameEnd >= 0) {
      const rawFrame = this.frameBuffer.slice(0, frameEnd).replace(/^\n+/, '')
      this.frameBuffer = this.frameBuffer.slice(frameEnd + 1)
      if (rawFrame) this.handleFrame(parseFrame(rawFrame))
      frameEnd = this.frameBuffer.indexOf('\0')
    }
  }

  private handleFrame(frame: StompFrame): void {
    if (frame.command === 'CONNECTED') {
      this.reconnectAttempt = 0
      this.callbacks.onState('connected')
      this.subscribe()
      return
    }

    if (frame.command === 'MESSAGE') {
      this.handleDashboardMessage(frame.body)
      return
    }

    if (frame.command === 'ERROR') {
      const message = `${frame.headers.message || ''} ${frame.body}`
      if (message.includes('未认证') || message.includes('失效') || message.includes('Unauthorized')) {
        this.stopped = true
        this.callbacks.onState('sessionExpired')
        handleSessionExpired()
        return
      }
      this.handleDisconnected()
    }
  }

  private handleDashboardMessage(body: string): void {
    try {
      const payload = JSON.parse(body) as Partial<RealtimeDashboard> & { overview?: Overview }
      if (payload.overview) this.callbacks.onUpdate({ overview: payload.overview, tasks: payload.tasks })
    } catch {
      this.callbacks.onState('disconnected')
    }
  }

  private handleDisconnected(): void {
    if (this.stopped || this.reconnectTimer) return

    this.callbacks.onState('disconnected')
    this.socket = undefined
    if (this.reconnectAttempt >= RECONNECT_DELAYS.length) {
      this.callbacks.onState('unavailable')
      return
    }

    const delay = RECONNECT_DELAYS[this.reconnectAttempt]
    this.reconnectAttempt += 1
    this.reconnectTimer = setTimeout(() => {
      this.reconnectTimer = 0 as unknown as number
      this.connect()
    }, delay)
  }
}

function getSocketUrl(): string {
  const websocketBase = getApiBaseUrl().replace(/^http:/, 'ws:').replace(/^https:/, 'wss:')
  return `${websocketBase}/ws/report/mini_app`
}

function parseFrame(rawFrame: string): StompFrame {
  const separator = rawFrame.indexOf('\n\n')
  const headerBlock = separator >= 0 ? rawFrame.slice(0, separator) : rawFrame
  const body = separator >= 0 ? rawFrame.slice(separator + 2) : ''
  const lines = headerBlock.replace(/\r/g, '').split('\n')
  const command = lines.shift() || ''
  const headers: Record<string, string> = {}
  lines.forEach(line => {
    const delimiter = line.indexOf(':')
    if (delimiter > 0) headers[line.slice(0, delimiter)] = line.slice(delimiter + 1)
  })
  return { command, headers, body }
}
