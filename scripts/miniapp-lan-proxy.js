'use strict'

const net = require('net')

const listenHost = process.argv[2] || '0.0.0.0'
const listenPort = Number(process.argv[3] || 8080)
const upstreamHost = process.argv[4] || '127.0.0.1'
const upstreamPort = Number(process.argv[5] || 8080)

const server = net.createServer(client => {
  const upstream = net.createConnection({ host: upstreamHost, port: upstreamPort })

  client.setNoDelay(true)
  upstream.setNoDelay(true)
  client.pipe(upstream)
  upstream.pipe(client)

  const closeBoth = () => {
    if (!client.destroyed) client.destroy()
    if (!upstream.destroyed) upstream.destroy()
  }

  client.on('error', closeBoth)
  upstream.on('error', closeBoth)
  client.on('close', closeBoth)
  upstream.on('close', closeBoth)
})

server.on('error', error => {
  process.stderr.write(`[miniapp-lan-proxy] ${error.stack || error.message}\n`)
  process.exitCode = 1
})

server.listen(listenPort, listenHost, () => {
  process.stdout.write(`[miniapp-lan-proxy] ${listenHost}:${listenPort} -> ${upstreamHost}:${upstreamPort}\n`)
})

const shutdown = () => server.close(() => process.exit(0))
process.on('SIGINT', shutdown)
process.on('SIGTERM', shutdown)
