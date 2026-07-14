import cors from "cors";
import crypto from "node:crypto";
import fs from "node:fs";
import { spawnSync } from "node:child_process";
import express from "express";
import QRCode from "qrcode";
import whatsappWebJs from "whatsapp-web.js";

const { Client, LocalAuth } = whatsappWebJs;

const app = express();
const port = Number(process.env.WHATSAPP_WEB_PORT ?? 3001);
const apiKey = process.env.WHATSAPP_WEB_API_KEY ?? "dev-whatsapp-web-key";
const sessionId = process.env.WHATSAPP_WEB_SESSION_ID ?? "demo-sales";
const backendWebhookUrl = process.env.WHATSAPP_WEB_BACKEND_WEBHOOK_URL ?? "http://backend-java:8080/api/v1/integrations/whatsapp-web/webhook";
const webhookSecret = process.env.WHATSAPP_WEB_WEBHOOK_SECRET ?? process.env.APP_WHATSAPP_WEB_WEBHOOK_SECRET ?? "dev-whatsapp-web-webhook-secret";
const realWhatsAppWebEnabled = String(process.env.WHATSAPP_WEB_REAL_ENABLED ?? "true").toLowerCase() === "true";
const autoConnectEnabled = String(process.env.WHATSAPP_WEB_AUTO_CONNECT ?? "true").toLowerCase() === "true";
const companyPhoneNumber = normalizeOptionalPhone(
  process.env.WHATSAPP_WEB_COMPANY_PHONE_NUMBER
    ?? process.env.WHATSAPP_WEB_DEFAULT_PHONE_NUMBER
    ?? "56900000000",
) ?? "56900000000";
const testCustomerPhoneNumber = normalizeOptionalPhone(
  process.env.WHATSAPP_WEB_TEST_CUSTOMER_PHONE_NUMBER
    ?? "56950954580",
) ?? "56950954580";
const defaultPhoneNumber = companyPhoneNumber;
const sessionDataPath = process.env.WHATSAPP_WEB_SESSION_DATA_PATH ?? "/app/.wwebjs_auth";
const cachePath = process.env.WHATSAPP_WEB_CACHE_PATH ?? "/app/.wwebjs_cache";
const chromeExecutablePath = process.env.WHATSAPP_WEB_CHROME_EXECUTABLE ?? process.env.PUPPETEER_EXECUTABLE_PATH ?? undefined;
const visualMode = String(process.env.WHATSAPP_WEB_VISUAL_MODE ?? "false").toLowerCase() === "true";
const headlessMode = String(process.env.WHATSAPP_WEB_HEADLESS ?? (visualMode ? "false" : "true")).toLowerCase() === "true";
const browserViewerUrl = process.env.WHATSAPP_WEB_BROWSER_VIEWER_URL ?? "http://localhost:6080/vnc.html?autoconnect=true&resize=scale";
const cleanProfileLocksOnStart = String(process.env.WHATSAPP_WEB_CLEAN_PROFILE_LOCKS_ON_START ?? "true").toLowerCase() === "true";
const killOrphanChromiumOnStart = String(process.env.WHATSAPP_WEB_KILL_ORPHAN_CHROMIUM_ON_START ?? "true").toLowerCase() === "true";
const webVersionCacheMode = String(process.env.WHATSAPP_WEB_WEB_VERSION_CACHE ?? "none").toLowerCase();
const chromiumInitRetries = Math.max(1, Number(process.env.WHATSAPP_WEB_INIT_RETRIES ?? 5));
const chromiumInitRetryDelayMs = Math.max(500, Number(process.env.WHATSAPP_WEB_INIT_RETRY_DELAY_MS ?? 10000));
const puppeteerProtocolTimeoutMs = Math.max(30000, Number(process.env.WHATSAPP_WEB_PUPPETEER_PROTOCOL_TIMEOUT_MS ?? 180000));
const puppeteerTimeoutMs = Math.max(30000, Number(process.env.WHATSAPP_WEB_PUPPETEER_TIMEOUT_MS ?? 180000));
const chromeExtraArgs = parseList(process.env.WHATSAPP_WEB_CHROME_EXTRA_ARGS ?? "");
const runtimeRecoveryDelayMs = Math.max(1000, Number(process.env.WHATSAPP_WEB_RUNTIME_RECOVERY_DELAY_MS ?? 5000));

const LOG_LEVEL = process.env.LOG_LEVEL ?? "info";
const LOG_JSON = String(process.env.LOG_JSON ?? "true").toLowerCase() === "true";

function generateCorrelationId() {
  return crypto.randomUUID();
}

function log(level, message, meta = {}) {
  if (shouldLog(level)) {
    const entry = {
      timestamp: new Date().toISOString(),
      level,
      service: "whatsapp-web-service",
      correlationId: meta.correlationId ?? "-",
      message,
      ...meta,
    };
    if (LOG_JSON) {
      console.log(JSON.stringify(entry));
    } else {
      console.log(`[${entry.timestamp}] [${level}] [${entry.correlationId}] ${message}`, meta);
    }
  }
}

function shouldLog(level) {
  const levels = { debug: 0, info: 1, warn: 2, error: 3 };
  return levels[level] >= levels[LOG_LEVEL];
}

function logDebug(message, meta) { log("debug", message, meta); }
function logInfo(message, meta) { log("info", message, meta); }
function logWarn(message, meta) { log("warn", message, meta); }
function logError(message, meta) { log("error", message, meta); }


const state = {
  sessionId,
  connectionStatus: "DISCONNECTED",
  phoneNumber: defaultPhoneNumber,
  qrCode: null,
  qrAttempts: 0,
  adapterMode: realWhatsAppWebEnabled ? "EXPERIMENTAL_REAL_WHATSAPP_WEB_JS" : "EXPERIMENTAL_STUB",
  browserMode: headlessMode ? "HEADLESS" : "VISUAL_BROWSER",
  browserViewerUrl: visualMode ? browserViewerUrl : null,
  testCustomerPhoneNumber,
  lastEventAt: new Date().toISOString(),
  lastError: null,
  runtimeReady: false,
};

let clientPromise = null;
let clientInstance = null;
let manualDisconnect = false;
const adapterSentMessageIds = new Map();
const adapterSentMessageRetentionMs = Math.max(30000, Number(process.env.WHATSAPP_WEB_ADAPTER_SENT_ID_RETENTION_MS ?? 300000));
const outboundExternalEmitDelayMs = Math.max(0, Number(process.env.WHATSAPP_WEB_OUTBOUND_EXTERNAL_EMIT_DELAY_MS ?? 1500));
let runtimeRecoveryTimer = null;

process.on("unhandledRejection", (reason) => {
  handleRuntimeError(reason, "process.unhandledRejection");
});

process.on("uncaughtException", (error) => {
  handleRuntimeError(error, "process.uncaughtException");
});

app.use(cors());
app.use(express.json({ limit: "2mb" }));

app.use((request, response, next) => {
  if (["/", "/health"].includes(request.path)) {
    return next();
  }

  if (request.header("X-API-Key") !== apiKey) {
    return response.status(401).json({
      code: "UNAUTHORIZED",
      message: "API key invalida para el adaptador experimental whatsapp-web.js.",
    });
  }

  return next();
});

function touch(eventStatus = null) {
  state.lastEventAt = new Date().toISOString();
  if (eventStatus) {
    state.connectionStatus = eventStatus;
  }
}

function isClientHealthy() {
  if (!clientInstance) return false;
  if (!state.runtimeReady) return false;
  if (state.connectionStatus !== "CONNECTED") return false;
  const info = clientInstance.info;
  if (!info || !info.wid) return false;
  return true;
}

function errorMessage(error) {
  if (error instanceof Error) {
    return error.message || String(error);
  }

  return String(error ?? "Error desconocido.");
}


function sanitizeLogValue(value) {
  if (value === null || value === undefined) {
    return value;
  }

  if (typeof value === "string") {
    if (/^\+?56\d{8,10}$/.test(value) || /@(?:c|g|lid)\.us$/.test(value) || /@lid$/.test(value)) {
      return "[IDENTIFICADOR_WHATSAPP_REDACTADO]";
    }
    if (value.length > 120) {
      return `${value.slice(0, 120)}...[TRUNCADO]`;
    }
    return value;
  }

  if (Array.isArray(value)) {
    return value.slice(0, 20).map((item) => sanitizeLogValue(item));
  }

  if (typeof value === "object") {
    const result = {};
    for (const [key, childValue] of Object.entries(value)) {
      if (["from", "to", "remote", "id", "phoneNumber", "qrCode", "token", "secret", "apiKey"].includes(key)) {
        result[key] = "[REDACTADO]";
      } else {
        result[key] = sanitizeLogValue(childValue);
      }
    }
    return result;
  }

  return value;
}

function isRecoverableBrowserRuntimeError(error) {
  const message = errorMessage(error).toLowerCase();

  return [
    "execution context was destroyed",
    "most likely because of a navigation",
    "target closed",
    "session closed",
    "protocol error",
    "navigation",
    "page crashed",
    "browser has disconnected",
    "chromium",
    "puppeteer",
  ].some((pattern) => message.includes(pattern));
}

function safeClientHandler(label, handler) {
  return (...args) => {
    Promise.resolve(handler(...args)).catch((error) => {
      handleRuntimeError(error, label);
    });
  };
}

function scheduleRuntimeRecovery(reason) {
  if (manualDisconnect || runtimeRecoveryTimer) {
    return;
  }

  runtimeRecoveryTimer = setTimeout(async () => {
    runtimeRecoveryTimer = null;
    const currentClient = clientInstance ?? (clientPromise ? await clientPromise.catch(() => null) : null);

    clientInstance = null;
    clientPromise = null;

    await destroyClientSafely(currentClient);
    removeChromiumProfileLocks(sessionDataPath);
    removeChromiumProfileLocks(cachePath);
    safeClearDirectoryContents("/tmp/whatsapp-web-profile");

    setState({
      connectionStatus: "SYNCING",
      runtimeReady: false,
      qrCode: null,
      lastError: `Recuperando sesion WhatsApp Web despues de: ${reason}`,
    }, { notifySession: true });

    startClientInBackground();
  }, runtimeRecoveryDelayMs);
}

function handleRuntimeError(error, source = "runtime") {
  const message = errorMessage(error);
  const recoverable = isRecoverableBrowserRuntimeError(error);

  console.error(`Error de ejecucion en ${source}. recoverable=${recoverable}`, error);

  setState({
    connectionStatus: recoverable ? "ERROR" : state.connectionStatus,
    runtimeReady: recoverable ? false : state.runtimeReady,
    lastError: message,
  }, { notifySession: true });

  if (recoverable) {
    scheduleRuntimeRecovery(message);
  }
}

function setState(nextState, { notifySession = false, notifyQr = false } = {}) {
  Object.assign(state, nextState);
  touch(nextState.connectionStatus ?? null);

  if (notifySession) {
    void notifyBackendSessionStatus();
  }

  if (notifyQr) {
    void notifyBackendQrUpdate();
  }
}

function normalizePhone(rawPhone) {
  const digits = String(rawPhone ?? "").replace(/\D/g, "");

  if (!digits) {
    const error = new Error("El telefono destino no contiene digitos validos.");
    error.statusCode = 400;
    throw error;
  }

  return digits.startsWith("00") ? digits.slice(2) : digits;
}

function normalizeOptionalPhone(rawPhone) {
  const digits = String(rawPhone ?? "").replace(/\D/g, "");
  if (!digits) {
    return null;
  }

  return digits.startsWith("00") ? digits.slice(2) : digits;
}

function normalizePhoneToChatId(rawPhone) {
  return `${normalizePhone(rawPhone)}@c.us`;
}

function normalizeDisplayPhone(rawPhone) {
  const digits = String(rawPhone ?? "").replace(/\D/g, "");
  return digits || defaultPhoneNumber;
}

function normalizeWhatsAppAddress(rawAddress) {
  const value = String(rawAddress ?? "").trim();

  if (!value) {
    return null;
  }

  const withoutDomain = value
    .replace("@c.us", "")
    .replace("@s.whatsapp.net", "")
    .replace("@lid", "")
    .replace("@g.us", "");

  const digits = normalizeOptionalPhone(withoutDomain);
  return digits ?? withoutDomain;
}

function extractClientPhone(client) {
  const wid = client?.info?.wid?._serialized ?? client?.info?.wid?.user ?? client?.info?.wid ?? defaultPhoneNumber;
  return normalizeDisplayPhone(wid);
}

function buildInboundWebhookPayload({
  from,
  to,
  body,
  externalMessageId,
  providerEventId,
  chatId,
  hasMedia = false,
  messageType = "text",
  timestamp = null,
  rawPayload = {},
}) {
  const normalizedFrom = normalizeWhatsAppAddress(from) ?? "unknown";
  const normalizedTo = normalizeWhatsAppAddress(to);
  const normalizedChatId = String(chatId ?? from ?? normalizedFrom).trim() || normalizedFrom;
  const normalizedBody = String(body ?? "").trim();

  return {
    from: normalizedFrom,
    body: normalizedBody,
    externalMessageId: externalMessageId || `wwebjs-in-${crypto.randomUUID()}`,
    providerEventId: providerEventId || externalMessageId || `provider-${crypto.randomUUID()}`,
    chatId: normalizedChatId,
    to: normalizedTo,
    hasMedia: Boolean(hasMedia),
    messageType: messageType || "text",
    timestamp: timestamp || new Date().toISOString(),
    rawPayload,
  };
}

function buildOutboundExternalWebhookPayload({
  from,
  to,
  body,
  externalMessageId,
  providerEventId,
  chatId,
  hasMedia = false,
  messageType = "text",
  timestamp = null,
  rawPayload = {},
}) {
  const normalizedFrom = normalizeWhatsAppAddress(from) ?? state.phoneNumber ?? defaultPhoneNumber;
  const normalizedTo = normalizeWhatsAppAddress(to);
  const normalizedChatId = String(chatId ?? to ?? normalizedTo ?? "unknown").trim();
  const normalizedBody = String(body ?? "").trim();

  return {
    from: normalizedFrom,
    to: normalizedTo,
    body: normalizedBody,
    externalMessageId: externalMessageId || `wwebjs-out-external-${crypto.randomUUID()}`,
    providerEventId: providerEventId || externalMessageId || `provider-${crypto.randomUUID()}`,
    chatId: normalizedChatId,
    hasMedia: Boolean(hasMedia),
    messageType: messageType || "text",
    timestamp: timestamp || new Date().toISOString(),
    source: "WHATSAPP_WEB_MANUAL",
    rawPayload,
  };
}

function publicState() {
  return {
    sessionId: state.sessionId,
    connectionStatus: state.connectionStatus,
    phoneNumber: state.phoneNumber,
    qrCode: state.qrCode,
    qrAttempts: state.qrAttempts,
    adapterMode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
    testCustomerPhoneNumber: state.testCustomerPhoneNumber,
    runtimeReady: state.runtimeReady,
    lastEventAt: state.lastEventAt,
    lastError: state.lastError,
  };
}

function actionState() {
  return {
    ...publicState(),
    acceptedAt: new Date().toISOString(),
  };
}

function signWebhook(timestamp, payload) {
  return `sha256=${crypto.createHmac("sha256", webhookSecret).update(`${timestamp}.${payload}`).digest("hex")}`;
}

async function emitWebhookEvent(eventType, payload, deliveryId = crypto.randomUUID()) {
  if (!backendWebhookUrl) {
    return;
  }

  const envelope = {
    eventType,
    deliveryId,
    occurredAt: new Date().toISOString(),
    sessionKey: sessionId,
    payload,
  };
  const rawBody = JSON.stringify(envelope);
  const timestamp = new Date().toISOString();

  try {
    const response = await fetch(backendWebhookUrl, {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
        "X-WhatsApp-Web-Timestamp": timestamp,
        "X-WhatsApp-Web-Signature": signWebhook(timestamp, rawBody),
        "X-WhatsApp-Web-Delivery-Id": deliveryId,
      },
      body: rawBody,
    });

    if (!response.ok) {
      const detail = await response.text();
      console.warn(`El backend rechazo el evento ${eventType} con estado ${response.status}.`, detail);
    }
  } catch (error) {
    console.warn(`No fue posible notificar el evento ${eventType} al backend Java.`, error);
  }
}

async function notifyBackendSessionStatus() {
  await emitWebhookEvent("SESSION_STATUS_CHANGED", {
    connectionStatus: state.connectionStatus,
    phoneNumber: state.phoneNumber,
    qrCode: state.qrCode,
    runtimeReady: state.runtimeReady,
    lastError: state.lastError,
    adapterMode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
  });
}

async function notifyBackendQrUpdate() {
  await emitWebhookEvent("QR_UPDATED", {
    connectionStatus: state.connectionStatus,
    phoneNumber: state.phoneNumber,
    qrCode: state.qrCode,
    qrAttempts: state.qrAttempts,
    runtimeReady: state.runtimeReady,
    lastError: state.lastError,
    adapterMode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
  });
}

function messageTimestampToIso(timestampSeconds) {
  const numericValue = Number(timestampSeconds);
  if (!Number.isFinite(numericValue)) {
    return new Date().toISOString();
  }

  return new Date(numericValue * 1000).toISOString();
}

function mapWwebjsAckStatus(ack) {
  switch (Number(ack)) {
    case 1:
      return "ACCEPTED";
    case 2:
      return "SENT";
    case 3:
      return "DELIVERED";
    case 4:
      return "READ";
    default:
      return "ACCEPTED";
  }
}

function parseList(rawValue) {
  return String(rawValue ?? "")
    .split(/[|\n]/)
    .map((value) => value.trim())
    .filter(Boolean);
}

function buildPuppeteerOptions() {
  const defaultArgs = [
    "--no-sandbox",
    "--disable-setuid-sandbox",
    "--disable-dev-shm-usage",
    "--disable-gpu",
    "--disable-extensions",
    "--disable-background-timer-throttling",
    "--disable-backgrounding-occluded-windows",
    "--disable-renderer-backgrounding",
    "--disable-sync",
    "--disable-translate",
    "--metrics-recording-only",
    "--mute-audio",
    "--no-default-browser-check",
    "--no-first-run",
    "--window-size=1366,768",
  ];

  return {
    headless: headlessMode,
    executablePath: chromeExecutablePath,
    args: [...new Set([...defaultArgs, ...chromeExtraArgs])],
    defaultViewport: null,
    protocolTimeout: puppeteerProtocolTimeoutMs,
    timeout: puppeteerTimeoutMs,
  };
}

function buildWebVersionCacheOptions() {
  if (webVersionCacheMode === "local") {
    return {
      type: "local",
      path: cachePath,
    };
  }

  if (webVersionCacheMode === "remote") {
    return {
      type: "remote",
      remotePath: process.env.WHATSAPP_WEB_WEB_VERSION_REMOTE_PATH,
    };
  }

  return {
    type: "none",
  };
}

function sleep(ms) {
  return new Promise((resolve) => {
    setTimeout(resolve, ms);
  });
}

function rememberAdapterSentMessageId(externalMessageId) {
  if (!externalMessageId) {
    return;
  }

  const expiresAt = Date.now() + adapterSentMessageRetentionMs;
  adapterSentMessageIds.set(externalMessageId, expiresAt);

  for (const [storedId, storedExpiresAt] of adapterSentMessageIds.entries()) {
    if (storedExpiresAt <= Date.now()) {
      adapterSentMessageIds.delete(storedId);
    }
  }
}

function wasSentByAdapter(externalMessageId) {
  if (!externalMessageId) {
    return false;
  }

  const expiresAt = adapterSentMessageIds.get(externalMessageId);
  if (!expiresAt) {
    return false;
  }

  if (expiresAt <= Date.now()) {
    adapterSentMessageIds.delete(externalMessageId);
    return false;
  }

  return true;
}

function safeClearDirectoryContents(directoryPath) {
  fs.mkdirSync(directoryPath, { recursive: true });

  for (const entry of fs.readdirSync(directoryPath)) {
    const targetPath = `${directoryPath}/${entry}`;

    try {
      fs.rmSync(targetPath, { recursive: true, force: true, maxRetries: 5, retryDelay: 250 });
    } catch (error) {
      console.warn(`No fue posible eliminar ${targetPath}. Se conserva para evitar detener el adaptador.`, error);
    }
  }
}

function removeChromiumProfileLocks(directoryPath) {
  if (!cleanProfileLocksOnStart || !fs.existsSync(directoryPath)) {
    return;
  }

  const lockFileNames = new Set([
    "SingletonLock",
    "SingletonSocket",
    "SingletonCookie",
    "DevToolsActivePort",
  ]);

  for (const entry of fs.readdirSync(directoryPath, { withFileTypes: true })) {
    const targetPath = `${directoryPath}/${entry.name}`;

    if (entry.isDirectory()) {
      removeChromiumProfileLocks(targetPath);
      continue;
    }

    if (lockFileNames.has(entry.name)) {
      try {
        fs.rmSync(targetPath, { force: true, maxRetries: 5, retryDelay: 250 });
      } catch (error) {
        console.warn(`No fue posible eliminar el bloqueo de Chromium ${targetPath}.`, error);
      }
    }
  }
}

function isGroupChatAddress(value) {
  return String(value ?? "").includes("@g.us");
}

function cleanupBeforeChromiumLaunch() {
  fs.mkdirSync(sessionDataPath, { recursive: true });
  fs.mkdirSync(cachePath, { recursive: true });

  if (killOrphanChromiumOnStart) {
    spawnSync("pkill", ["-f", "chromium"], { stdio: "ignore" });
  }

  removeChromiumProfileLocks(sessionDataPath);
  removeChromiumProfileLocks(cachePath);
  removeChromiumProfileLocks("/tmp/whatsapp-web-profile");
}

async function destroyClientSafely(client) {
  if (!client || typeof client.destroy !== "function") {
    return;
  }

  try {
    await client.destroy();
  } catch (error) {
    console.warn("No fue posible destruir completamente la instancia whatsapp-web.js.", error);
  }
}

async function notifyBackendInbound(message) {
  if (!message) {
    return;
  }

  if (message.fromMe) {
    console.debug("Mensaje propio ignorado por el flujo entrante para evitar bucles de respuesta automatica.", sanitizeLogValue({
      to: message.to,
      type: message.type,
      id: message.id?._serialized ?? message.id?.id,
    }));
    return;
  }

  if (isGroupChatAddress(message.from) || isGroupChatAddress(message.to) || isGroupChatAddress(message.id?.remote)) {
    console.info("Mensaje de grupo ignorado por el flujo entrante del asistente para evitar respuestas fuera de contexto.", sanitizeLogValue({
      from: message.from,
      to: message.to,
      remote: message.id?.remote,
      id: message.id?._serialized ?? message.id?.id,
    }));
    return;
  }

  const normalizedBody = String(message.body ?? "").trim();
  const messageType = message.type || "text";
  if (!normalizedBody && !message.hasMedia) {
    console.info("Mensaje entrante ignorado por no contener texto ni multimedia util.", sanitizeLogValue({
      from: message.from,
      type: messageType,
      id: message.id?._serialized ?? message.id?.id,
    }));
    return;
  }

  const externalMessageId = message.id?._serialized ?? message.id?.id ?? `wwebjs-in-${crypto.randomUUID()}`;
  const companyAddress = message.to ?? state.phoneNumber ?? defaultPhoneNumber;
  const payload = buildInboundWebhookPayload({
    from: message.from,
    to: companyAddress,
    body: normalizedBody,
    externalMessageId,
    providerEventId: externalMessageId,
    chatId: message.from,
    hasMedia: message.hasMedia,
    messageType,
    timestamp: messageTimestampToIso(message.timestamp),
    rawPayload: {
      id: externalMessageId,
      from: message.from,
      to: message.to,
      author: message.author,
      fromMe: message.fromMe,
      type: message.type,
      hasMedia: message.hasMedia,
      timestamp: message.timestamp,
      deviceType: message.deviceType,
    },
  });

  await emitWebhookEvent("MESSAGE_RECEIVED", payload);
}

async function notifyBackendOutboundExternal(message) {
  if (!message || !message.fromMe) {
    return;
  }

  if (isGroupChatAddress(message.from) || isGroupChatAddress(message.to) || isGroupChatAddress(message.id?.remote)) {
    console.info("Mensaje saliente de grupo ignorado por el registro manual del asistente.", sanitizeLogValue({
      from: message.from,
      to: message.to,
      remote: message.id?.remote,
      id: message.id?._serialized ?? message.id?.id,
    }));
    return;
  }

  const externalMessageId = message.id?._serialized ?? message.id?.id ?? `wwebjs-out-external-${crypto.randomUUID()}`;
  if (outboundExternalEmitDelayMs > 0) {
    await sleep(outboundExternalEmitDelayMs);
  }

  if (wasSentByAdapter(externalMessageId)) {
    console.debug("Mensaje saliente propio no se registra como manual porque fue emitido desde el adaptador.", sanitizeLogValue({
      to: message.to,
      type: message.type,
      id: externalMessageId,
    }));
    return;
  }

  const normalizedBody = String(message.body ?? "").trim();
  const messageType = message.type || "text";
  if (!normalizedBody && !message.hasMedia) {
    console.info("Mensaje saliente manual ignorado por no contener texto ni multimedia util.", sanitizeLogValue({
      to: message.to,
      type: messageType,
      id: externalMessageId,
    }));
    return;
  }

  const customerAddress = message.to ?? message.from ?? message.id?.remote;
  const companyAddress = message.from ?? state.phoneNumber ?? defaultPhoneNumber;
  const normalizedCustomer = normalizeWhatsAppAddress(customerAddress);
  const normalizedCompany = normalizeWhatsAppAddress(companyAddress);

  if (!normalizedCustomer) {
    console.warn("Mensaje saliente manual no enviado al backend porque no fue posible resolver el telefono destino.", sanitizeLogValue({
      from: message.from,
      to: message.to,
      id: externalMessageId,
    }));
    return;
  }

  const payload = buildOutboundExternalWebhookPayload({
    from: normalizedCompany,
    to: normalizedCustomer,
    body: normalizedBody,
    externalMessageId,
    providerEventId: externalMessageId,
    chatId: customerAddress,
    hasMedia: message.hasMedia,
    messageType,
    timestamp: messageTimestampToIso(message.timestamp),
    rawPayload: {
      id: externalMessageId,
      from: message.from,
      to: message.to,
      author: message.author,
      fromMe: message.fromMe,
      type: message.type,
      hasMedia: message.hasMedia,
      timestamp: message.timestamp,
      deviceType: message.deviceType,
      source: "WHATSAPP_WEB_MANUAL",
    },
  });

  await emitWebhookEvent("MESSAGE_SENT_EXTERNAL", payload);
}

function createWhatsAppClient() {
  return new Client({
    authStrategy: new LocalAuth({
      clientId: sessionId,
      dataPath: sessionDataPath,
    }),
    puppeteer: buildPuppeteerOptions(),
    qrMaxRetries: 0,
    takeoverOnConflict: true,
    takeoverTimeoutMs: 0,
    webVersionCache: buildWebVersionCacheOptions(),
  });
}

function registerClientHandlers(client) {
  client.on("qr", safeClientHandler("client.qr", async (qr) => {
    try {
      const qrDataUrl = await QRCode.toDataURL(qr, { margin: 1, width: 320 });
      setState({
        qrCode: qrDataUrl,
        qrAttempts: state.qrAttempts + 1,
        connectionStatus: "QR_PENDING",
        runtimeReady: false,
        lastError: null,
      }, { notifySession: true, notifyQr: true });
    } catch (error) {
      setState({
        qrCode: qr,
        qrAttempts: state.qrAttempts + 1,
        connectionStatus: "QR_PENDING",
        runtimeReady: false,
        lastError: error?.message ?? String(error),
      }, { notifySession: true, notifyQr: true });
    }
  }));

  client.on("loading_screen", (percent, message) => {
    setState({
      connectionStatus: "SYNCING",
      runtimeReady: false,
      lastError: null,
      loadingPercent: percent,
      loadingMessage: message,
    }, { notifySession: true });
  });

  client.on("authenticated", () => {
    setState({
      connectionStatus: "AUTHENTICATED",
      runtimeReady: false,
      lastError: null,
    }, { notifySession: true });
  });

  client.on("auth_failure", (message) => {
    setState({
      connectionStatus: "ERROR",
      runtimeReady: false,
      lastError: message || "Fallo la autenticacion de WhatsApp Web.",
    }, { notifySession: true });
  });

  client.on("ready", () => {
    setState({
      connectionStatus: "CONNECTED",
      runtimeReady: true,
      qrCode: null,
      phoneNumber: extractClientPhone(client),
      lastError: null,
    }, { notifySession: true });
  });

  client.on("disconnected", (reason) => {
    clientInstance = null;
    clientPromise = null;

    setState({
      connectionStatus: manualDisconnect ? "DISCONNECTED" : "ERROR",
      runtimeReady: false,
      qrCode: null,
      lastError: manualDisconnect ? null : String(reason ?? "WhatsApp Web desconectado."),
    }, { notifySession: true });

    if (!manualDisconnect) {
      scheduleRuntimeRecovery(String(reason ?? "WhatsApp Web desconectado."));
    }
  });

  client.on("message", safeClientHandler("client.message", async (message) => {
    setState({ connectionStatus: "CONNECTED", runtimeReady: true, lastError: null }, { notifySession: true });
    await notifyBackendInbound(message);
  }));

  client.on("message_create", safeClientHandler("client.message_create", async (message) => {
    if (!message?.fromMe) {
      return;
    }

    setState({ connectionStatus: "CONNECTED", runtimeReady: true, lastError: null }, { notifySession: true });
    await notifyBackendOutboundExternal(message);
  }));

  client.on("message_ack", safeClientHandler("client.message_ack", async (message, ack) => {
    if (!message?.fromMe) {
      return;
    }

    const externalMessageId = message.id?._serialized ?? message.id?.id;
    if (!externalMessageId) {
      return;
    }

    await emitWebhookEvent("MESSAGE_ACK_UPDATED", {
      externalMessageId,
      providerEventId: `ack-${externalMessageId}-${ack}`,
      status: mapWwebjsAckStatus(ack),
    });
  }));

}

async function buildClient() {
  cleanupBeforeChromiumLaunch();

  let lastError = null;

  for (let attempt = 1; attempt <= chromiumInitRetries; attempt += 1) {
    const client = createWhatsAppClient();
    registerClientHandlers(client);
    clientInstance = client;

    try {
      await client.initialize();
      return client;
    } catch (error) {
      lastError = error;
      clientInstance = null;

      setState({
        connectionStatus: "ERROR",
        runtimeReady: false,
        lastError: `Intento ${attempt}/${chromiumInitRetries}: ${error?.message ?? String(error)}`,
      }, { notifySession: true });

      await destroyClientSafely(client);
      removeChromiumProfileLocks(sessionDataPath);
      removeChromiumProfileLocks(cachePath);
      safeClearDirectoryContents("/tmp/whatsapp-web-profile");

      if (attempt < chromiumInitRetries) {
        console.warn(`whatsapp-web.js no inicializo Chromium en el intento ${attempt}/${chromiumInitRetries}. Reintentando...`, error);
        await sleep(chromiumInitRetryDelayMs);
        cleanupBeforeChromiumLaunch();
      }
    }
  }

  throw lastError ?? new Error("No fue posible inicializar whatsapp-web.js.");
}

async function ensureClient() {
  if (!realWhatsAppWebEnabled) {
    return null;
  }

  if (clientInstance) {
    return clientInstance;
  }

  if (!clientPromise) {
    manualDisconnect = false;
    setState({
      adapterMode: "EXPERIMENTAL_REAL_WHATSAPP_WEB_JS",
      browserMode: headlessMode ? "HEADLESS" : "VISUAL_BROWSER",
      browserViewerUrl: visualMode ? browserViewerUrl : null,
      connectionStatus: "SYNCING",
      qrCode: null,
      qrAttempts: 0,
      runtimeReady: false,
      lastError: null,
    }, { notifySession: true });

    clientPromise = buildClient()
      .then((client) => {
        clientInstance = client;
        return client;
      })
      .catch((error) => {
        clientPromise = null;
        clientInstance = null;
        setState({
          connectionStatus: "ERROR",
          runtimeReady: false,
          lastError: error?.message ?? String(error),
        }, { notifySession: true });
        throw error;
      });
  }

  return clientPromise;
}

function startClientInBackground() {
  if (!realWhatsAppWebEnabled || clientPromise || clientInstance) {
    return;
  }

  void ensureClient().catch((error) => {
    console.warn("whatsapp-web.js no pudo completar la conexion en segundo plano.", error);
  });
}

async function disconnectClient({ clearSession = true } = {}) {
  manualDisconnect = true;
  const pendingClient = clientInstance ?? (clientPromise ? await clientPromise.catch(() => null) : null);

  if (pendingClient && clearSession && state.runtimeReady && typeof pendingClient.logout === "function") {
    try {
      await pendingClient.logout();
    } catch (error) {
      console.warn("No fue posible cerrar la sesion remota de WhatsApp Web. Se continuara con el cierre local.", error);
    }
  }

  await destroyClientSafely(pendingClient);
  await sleep(500);

  clientInstance = null;
  clientPromise = null;

  if (clearSession) {
    // sessionDataPath suele ser un volumen Docker montado en /app/.wwebjs_auth.
    // No se debe borrar el directorio raiz montado porque Docker lo mantiene ocupado.
    // Solo se limpian sus archivos internos para forzar un nuevo QR.
    safeClearDirectoryContents(sessionDataPath);
    safeClearDirectoryContents("/tmp/whatsapp-web-profile");
  }

  setState({
    connectionStatus: "DISCONNECTED",
    runtimeReady: false,
    phoneNumber: defaultPhoneNumber,
    qrCode: null,
    qrAttempts: 0,
    lastError: null,
  }, { notifySession: true });
}

async function resolveChatId(client, rawPhone) {
  const value = String(rawPhone ?? "").trim();

  if (/@(c\.us|s\.whatsapp\.net|lid|g\.us)$/i.test(value)) {
    return value;
  }

  const serializedIdMatch = value.match(/(?:true|false)_([^_]+@(c\.us|s\.whatsapp\.net|lid|g\.us))_/i);
  if (serializedIdMatch?.[1]) {
    return serializedIdMatch[1];
  }

  const digits = normalizePhone(value);

  if (typeof client.getNumberId === "function") {
    try {
      const numberId = await client.getNumberId(digits);
      if (numberId?._serialized) {
        return numberId._serialized;
      }
    } catch (_error) {
      // Se usa el identificador directo como fallback.
    }
  }

  return normalizePhoneToChatId(digits);
}

async function sendMessage({ businessId, to, body }) {
  if (!businessId || !to || !body) {
    const error = new Error("businessId, to y body son obligatorios.");
    error.statusCode = 400;
    throw error;
  }

  if (!realWhatsAppWebEnabled) {
    setState({ connectionStatus: "CONNECTED" }, { notifySession: true });
    return {
      messageId: `stub-${Date.now()}`,
      status: "QUEUED",
      acceptedAt: state.lastEventAt,
      chatId: normalizePhoneToChatId(to),
      adapterMode: state.adapterMode,
    };
  }

  if (!clientInstance || !state.runtimeReady) {
    startClientInBackground();
    const error = new Error("La sesion whatsapp-web.js aun no esta conectada. Escanea el QR y vuelve a intentar el envio.");
    error.statusCode = 503;
    throw error;
  }

  const client = clientInstance;
  const chatId = await resolveChatId(client, to);
  const result = await client.sendMessage(chatId, String(body));
  const externalId = result?.id?._serialized ?? result?.id?.id ?? `wwebjs-out-${Date.now()}`;
  rememberAdapterSentMessageId(externalId);

  setState({
    connectionStatus: "CONNECTED",
    runtimeReady: true,
    phoneNumber: extractClientPhone(client),
    lastError: null,
  }, { notifySession: true });

  return {
    messageId: externalId,
    status: "SENT",
    acceptedAt: new Date().toISOString(),
    chatId,
    adapterMode: state.adapterMode,
  };
}

app.get("/", (_request, response) => {
  response.json({
    service: "whatsapp-webjs-service",
    compatibilityServiceName: "whatsapp-web-service",
    mode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
    protected: true,
    publicEndpoints: ["/", "/health"],
    connectionStatus: state.connectionStatus,
    runtimeReady: state.runtimeReady,
    message: "El adaptador whatsapp-web.js requiere X-API-Key para sus endpoints operativos.",
  });
});

app.get("/health", (_request, response) => {
  const healthy = realWhatsAppWebEnabled ? isClientHealthy() : true;
  const statusCode = healthy ? 200 : 503;
  response.status(statusCode).json({
    status: healthy ? "UP" : "DOWN",
    service: "whatsapp-webjs-service",
    compatibilityServiceName: "whatsapp-web-service",
    mode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
    connectionStatus: state.connectionStatus,
    runtimeReady: state.runtimeReady,
    clientHealthy: healthy,
    lastError: state.lastError,
    timestamp: new Date().toISOString(),
  });
});

app.get(["/api/v1/session/status", "/whatsapp/status", "/whatsapp-web/status", "/baileys/status"], (_request, response) => {
  response.json(publicState());
});

app.get(["/api/v1/session/browser", "/whatsapp/browser", "/whatsapp-web/browser"], (_request, response) => {
  response.json({
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
    visualMode,
    headlessMode,
    message: visualMode
      ? "Abre browserViewerUrl para ver la ventana Chromium con WhatsApp Web dentro del contenedor."
      : "El modo visual esta desactivado. Ejecuta con WHATSAPP_WEB_VISUAL_MODE=true y WHATSAPP_WEB_HEADLESS=false.",
  });
});

app.get(["/api/v1/session/qr", "/whatsapp/qr", "/whatsapp-web/qr", "/baileys/qr"], (_request, response) => {
  response.json({
    qrCode: state.qrCode,
    connectionStatus: state.connectionStatus,
    qrAttempts: state.qrAttempts,
    lastEventAt: state.lastEventAt,
    adapterMode: state.adapterMode,
    browserMode: state.browserMode,
    browserViewerUrl: state.browserViewerUrl,
  });
});

app.post(["/api/v1/session/connect", "/whatsapp/connect", "/whatsapp/reconnect", "/whatsapp-web/connect", "/whatsapp-web/reconnect", "/baileys/connect", "/baileys/reconnect"], (_request, response) => {
  startClientInBackground();
  response.status(202).json(actionState());
});

app.post(["/api/v1/session/refresh-qr", "/whatsapp/refresh-qr", "/whatsapp-web/refresh-qr", "/baileys/refresh-qr"], async (_request, response) => {
  try {
    await disconnectClient({ clearSession: true });
    setState({ connectionStatus: "SYNCING" }, { notifySession: true });
    startClientInBackground();
    response.status(202).json(actionState());
  } catch (error) {
    setState({ connectionStatus: "ERROR", runtimeReady: false, lastError: error?.message ?? String(error) }, { notifySession: true });
    response.status(500).json({
      code: "WHATSAPP_WEBJS_REFRESH_QR_ERROR",
      message: error?.message ?? "No fue posible regenerar el QR de WhatsApp Web.",
      status: publicState(),
    });
  }
});

app.post(["/api/v1/session/disconnect", "/whatsapp/disconnect", "/whatsapp-web/disconnect", "/baileys/disconnect"], async (_request, response) => {
  try {
    await disconnectClient({ clearSession: true });
    response.status(202).json(actionState());
  } catch (error) {
    setState({ connectionStatus: "ERROR", runtimeReady: false, lastError: error?.message ?? String(error) }, { notifySession: true });
    response.status(500).json({
      code: "WHATSAPP_WEBJS_DISCONNECT_ERROR",
      message: error?.message ?? "No fue posible desconectar WhatsApp Web.",
      status: publicState(),
    });
  }
});

app.post(["/api/v1/messages/send", "/whatsapp/send-text", "/whatsapp-web/send-text", "/baileys/send-text"], async (request, response) => {
  try {
    const delivery = await sendMessage(request.body ?? {});
    response.status(202).json(delivery);
  } catch (error) {
    const statusCode = error.statusCode ?? 503;
    setState({ lastError: error?.message ?? String(error), connectionStatus: statusCode === 400 ? state.connectionStatus : "ERROR" }, { notifySession: true });
    response.status(statusCode).json({
      code: statusCode === 400 ? "VALIDATION_ERROR" : "WHATSAPP_WEBJS_SEND_ERROR",
      message: error?.message ?? "No fue posible enviar el mensaje por whatsapp-web.js. Verifica que el QR este escaneado y la sesion conectada.",
      status: publicState(),
    });
  }
});

app.post(["/api/v1/messages/simulate-inbound", "/whatsapp/simulate-inbound", "/whatsapp/simulate-message", "/whatsapp-web/simulate-inbound", "/baileys/simulate-inbound"], async (request, response) => {
  const {
    from = state.testCustomerPhoneNumber,
    to = state.phoneNumber,
    body = "Mensaje entrante simulado desde whatsapp-web.js.",
  } = request.body ?? {};

  const deliveryId = crypto.randomUUID();
  const externalMessageId = `sim-${crypto.randomUUID()}`;

  const payload = buildInboundWebhookPayload({
    from,
    to,
    body,
    externalMessageId,
    providerEventId: externalMessageId,
    chatId: normalizePhoneToChatId(from),
    hasMedia: false,
    messageType: "text",
    timestamp: new Date().toISOString(),
    rawPayload: request.body ?? {},
  });

  await emitWebhookEvent("MESSAGE_RECEIVED", payload, deliveryId);

  response.status(202).json({
    status: "ACCEPTED",
    deliveryId,
    acceptedAt: new Date().toISOString(),
  });
});

if (autoConnectEnabled) {
  startClientInBackground();
}

const server = app.listen(port, "0.0.0.0", () => {
  logInfo(`whatsapp-webjs-service ${state.adapterMode} listening on 0.0.0.0:${port}`);
  logInfo(`[health] GET http://0.0.0.0:${port}/health`);
  logInfo(`[config] visualMode=${visualMode} headlessMode=${headlessMode} autoConnect=${autoConnectEnabled} realEnabled=${realWhatsAppWebEnabled}`);
  if (state.browserViewerUrl) {
    logInfo(`[visual] browser viewer at ${state.browserViewerUrl}`);
  }
  logInfo(`[paths] sessionData=${sessionDataPath} cache=${cachePath} chrome=${chromeExecutablePath}`);
});

async function gracefulShutdown(signal) {
  logInfo(`Received ${signal}, starting graceful shutdown`);
  manualDisconnect = true;
  if (runtimeRecoveryTimer) {
    clearTimeout(runtimeRecoveryTimer);
    runtimeRecoveryTimer = null;
  }
  server.close(() => {
    logInfo("HTTP server closed");
  });
  const pendingClient = clientInstance ?? (clientPromise ? await clientPromise.catch(() => null) : null);
  await destroyClientSafely(pendingClient);
  logInfo("WhatsApp client destroyed");
  process.exit(0);
}

process.on("SIGTERM", () => gracefulShutdown("SIGTERM"));
process.on("SIGINT", () => gracefulShutdown("SIGINT"));
