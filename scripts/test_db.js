const { execSync } = require('child_process');
try {
  const r = execSync('docker exec asistente-postgres psql -U assistant -d asistente_whatsapp -t -A -c "SELECT 1 as test;"', { encoding: 'utf8', timeout: 5000 });
  console.log('DB OK:', r.trim());
} catch(e) {
  console.error('Error:', e.message);
}
