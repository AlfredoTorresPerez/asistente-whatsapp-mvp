#!/usr/bin/env node
/**
 * recover-session.js
 *
 * Recovery script for WhatsApp Web session data.
 * Restores .wwebjs_auth from .wwebjs_auth.backup if the main session is corrupted.
 *
 * Usage:
 *   docker compose exec whatsapp-web-service node recover-session.js
 *
 * Or mount the volume and run locally:
 *   node recover-session.js --path /path/to/.wwebjs_auth
 */

import fs from "node:fs";
import path from "node:path";

const SESSION_PATH = process.env.WHATSAPP_WEB_SESSION_DATA_PATH || "/app/.wwebjs_auth";
const BACKUP_PATH = `${SESSION_PATH}.backup`;

function log(message) {
  console.log(`[recover-session] ${message}`);
}

function sessionIsCorrupt(sessionDir) {
  if (!fs.existsSync(sessionDir)) {
    return true;
  }

  try {
    const entries = fs.readdirSync(sessionDir);
    if (entries.length === 0) {
      return true;
    }

    const hasPrefFile = entries.some((e) => e.endsWith("json") || e.includes("pref"));
    const hasStoreFile = entries.some((e) => e.includes("store") || e.includes("data"));

    if (!hasPrefFile && !hasStoreFile) {
      return true;
    }

    return false;
  } catch {
    return true;
  }
}

function restoreFromBackup() {
  if (!fs.existsSync(BACKUP_PATH)) {
    log("No backup found at " + BACKUP_PATH);
    return false;
  }

  try {
    if (fs.existsSync(SESSION_PATH)) {
      fs.rmSync(SESSION_PATH, { recursive: true, force: true });
    }

    fs.cpSync(BACKUP_PATH, SESSION_PATH, { recursive: true });
    log("Session restored from backup: " + BACKUP_PATH + " -> " + SESSION_PATH);
    return true;
  } catch (error) {
    log("Failed to restore session: " + error.message);
    return false;
  }
}

function main() {
  const args = process.argv.slice(2);
  const pathIndex = args.indexOf("--path");
  const sessionDir = pathIndex !== -1 ? args[pathIndex + 1] : SESSION_PATH;
  const backupDir = pathIndex !== -1 ? `${args[pathIndex + 1]}.backup` : BACKUP_PATH;

  log("Session path: " + sessionDir);
  log("Backup path: " + backupDir);

  if (sessionIsCorrupt(sessionDir)) {
    log("Session data is missing or corrupt. Attempting recovery...");
    if (restoreFromBackup()) {
      log("Session recovered successfully.");
      process.exit(0);
    } else {
      log("Could not recover session. A new QR will be required.");
      process.exit(1);
    }
  } else {
    log("Session data looks healthy. No recovery needed.");
    process.exit(0);
  }
}

main();
