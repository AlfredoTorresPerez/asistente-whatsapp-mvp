const { execSync } = require('child_process');

async function login() {
  const resp = await fetch('http://localhost:8080/api/v1/auth/login', {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email: 'admin@demo.cl', password: 'Cambiar123!' })
  });
  if (!resp.ok) throw new Error('Login failed');
  const data = await resp.json();
  return data.accessToken;
}

async function sendMessage(token, phone, body) {
  const resp = await fetch('http://localhost:8080/api/v1/test/whatsapp-inbound', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${token}`
    },
    body: JSON.stringify({ from: phone, body })
  });
  if (!resp.ok) throw new Error(`Send failed: ${resp.status}`);
}

function getResponse(phone) {
  const safePhone = phone.replace(/'/g, "''");
  const r = execSync(
    `docker exec asistente-postgres psql -U assistant -d asistente_whatsapp -t -A -c "SELECT m.body FROM message m JOIN conversation c ON m.conversation_id = c.id WHERE c.customer_phone = '${safePhone}' AND m.direction = 'OUTBOUND' ORDER BY m.created_at DESC LIMIT 1;"`,
    { encoding: 'utf8', timeout: 10000 }
  );
  return r.trim();
}

async function sleep(ms) {
  return new Promise(resolve => setTimeout(resolve, ms));
}

async function main() {
  const phone = '+56910009901';
  console.log('Logging in...');
  const token = await login();
  console.log('Token obtained');

  console.log(`Sending message from ${phone}...`);
  await sendMessage(token, phone, 'Hola, ¿qué servicios ofrecen?');

  console.log('Waiting for response...');
  let response = null;
  for (let i = 0; i < 15; i++) {
    await sleep(2000);
    response = getResponse(phone);
    if (response && response.length > 0) {
      break;
    }
  }

  if (response) {
    console.log('RESPONSE:', response);
  } else {
    console.log('NO RESPONSE after 30s');
  }
}

main().catch(e => console.error('FATAL:', e.message));
