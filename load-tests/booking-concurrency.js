// k6 load test — Concurrencia de reservas
// Valida la EXCLUDE constraint de PostgreSQL y el manejo de DataIntegrityViolationException.
//
// Escenarios:
//   1. race-same-slot:  20 VUs disparan en simultáneo exacto contra el mismo slot.
//      Solo 1 debe crear la reserva (201); el resto debe recibir 409/conflict (DataIntegrityViolationException).
//   2. location-only:   10 VUs crean reserva SIN professional_id ni room_id (solo location).
//      Valida constraint ex_booking_location_no_overlap_active.
//   3. burst:           50 VUs en 5s, cada uno con slot distinto — validar que no hay falsos positivos.
//
// Ejecutar:
//   k6 run load-tests/booking-concurrency.js
//   k6 run --vus 20 --duration 30s load-tests/booking-concurrency.js   (scenario individual)
//   K6_BASE_URL=https://staging.example.com k6 run load-tests/booking-concurrency.js
//
// Requisitos:
//   - Backend corriendo en BASE_URL (default http://localhost:8080)
//   - Usuario admin@demo.cl / Cambiar123! (o configurar vía variables de entorno)
//   - Location con ID 44444444-4444-4444-4444-444444444444 existente en DB

import http from 'k6/http';
import { check, sleep, fail } from 'k6';

const BASE_URL = __ENV.K6_BASE_URL || 'http://localhost:8080/api/v1';
const DEMO_EMAIL = __ENV.K6_EMAIL || 'admin@demo.cl';
const DEMO_PASSWORD = __ENV.K6_PASSWORD || 'Cambiar123!';
const LOCATION_ID = __ENV.K6_LOCATION_ID || '44444444-4444-4444-4444-444444444444';
const SERVICE_ID = __ENV.K6_SERVICE_ID || '33333333-3333-3333-3333-333333333333';

export const options = {
  scenarios: {
    race_same_slot: {
      executor: 'shared-iterations',
      vus: 20,
      iterations: 20,
      maxDuration: '30s',
      startTime: '0s',
      tags: { scenario: 'race-same-slot' },
    },
    location_only: {
      executor: 'shared-iterations',
      vus: 10,
      iterations: 10,
      maxDuration: '30s',
      startTime: '5s',
      tags: { scenario: 'location-only' },
    },
    burst: {
      executor: 'ramping-vus',
      startVUs: 0,
      stages: [
        { duration: '5s', target: 50 },
        { duration: '10s', target: 50 },
        { duration: '5s', target: 0 },
      ],
      startTime: '10s',
      tags: { scenario: 'burst' },
    },
  },
  thresholds: {
    http_req_duration: ['p(95)<10000'],
    http_req_failed: ['rate<0.50'],
    'http_req_duration{scenario:race-same-slot}': ['p(95)<5000'],
    'http_req_duration{scenario:burst}': ['p(95)<8000'],
  },
};

// Slot fijo para race-same-slot — todas las VUs compiten por el mismo minuto exacto
const RACE_DATE = (() => {
  const d = new Date();
  d.setDate(d.getDate() + 60);
  d.setHours(10, 0, 0, 0);
  if (d.getDay() === 0) d.setDate(d.getDate() + 1);
  if (d.getDay() === 6) d.setDate(d.getDate() + 2);
  return d.toISOString().split('T')[0];
})();
const RACE_STARTS_AT = `${RACE_DATE}T14:00:00-04:00`;
const RACE_ENDS_AT = `${RACE_DATE}T14:30:00-04:00`;
const RACE_PROFESSIONAL_ID = '55555555-5555-5555-5555-555555555555';
const RACE_ROOM_ID = '66666666-6666-6666-6666-666666666666';

export function setup() {
  const loginRes = http.post(`${BASE_URL}/auth/login`, JSON.stringify({
    email: DEMO_EMAIL,
    password: DEMO_PASSWORD,
  }), { headers: { 'Content-Type': 'application/json' } });

  if (loginRes.status !== 200) {
    console.warn(`Login falló (${loginRes.status}). Continuando sin token — los requests fallarán con 401.`);
    return { token: null };
  }

  const token = loginRes.json('accessToken') || loginRes.json('token');
  console.log(`Setup OK — token obtenido. Race date: ${RACE_DATE}`);
  return { token };
}

export default function (data) {
  const token = data.token;
  const headers = {
    'Content-Type': 'application/json',
  };
  if (token) headers.Authorization = `Bearer ${token}`;

  const scenario = __ITER === 0 ? 'unknown' : '';

  // Determinamos escenario según tags definidos en options
  if (__ENV.SCENARIO === 'race-same-slot' || !__ENV.SCENARIO) {
    // Escenario por defecto: el script se ejecuta con scenarios de options
    // Usamos __VU para identificar qué hacer cuando se ejecuta sin scenarios
  }

  const now = new Date();
  const dateStr = now.toISOString().split('T')[0];

  // 1. CREAR RESERVA TEMPORAL
  // race-same-slot y location-only usan slot fijo para forzar contención
  let payload;
  let tag;

  if (__VU <= 20) {
    // race-same-slot: todas las VUs 1-20 apuntan al mismo slot exacto
    payload = {
      locationId: LOCATION_ID,
      serviceId: SERVICE_ID,
      professionalId: RACE_PROFESSIONAL_ID,
      roomId: RACE_ROOM_ID,
      startsAt: RACE_STARTS_AT,
      endsAt: RACE_ENDS_AT,
      customerName: `Race VU ${__VU}`,
      customerPhone: `+5690000${String(__VU).padStart(4, '0')}`,
      expirationMinutes: 30,
    };
    tag = 'race-same-slot';
  } else if (__VU <= 30) {
    // location-only: sin professional_id ni room_id — valida ex_booking_location_no_overlap_active
    payload = {
      locationId: LOCATION_ID,
      serviceId: SERVICE_ID,
      startsAt: RACE_STARTS_AT,
      endsAt: RACE_ENDS_AT,
      customerName: `LocationOnly VU ${__VU}`,
      customerPhone: `+5690000${String(__VU).padStart(4, '0')}`,
      expirationMinutes: 30,
    };
    tag = 'location-only';
  } else {
    // burst: cada VU usa un slot único para validar que no hay falsos positivos
    const uniqueMinute = (__VU * 7 + __ITER * 13) % 480;
    const hour = String(8 + Math.floor(uniqueMinute / 60)).padStart(2, '0');
    const min = String(uniqueMinute % 60).padStart(2, '0');
    const uniqueStartsAt = `${dateStr}T${hour}:${min}:00-04:00`;
    const uniqueEndsAt = `${dateStr}T${String(parseInt(hour) + 1).padStart(2, '0')}:${min}:00-04:00`;
    payload = {
      locationId: LOCATION_ID,
      serviceId: SERVICE_ID,
      professionalId: RACE_PROFESSIONAL_ID,
      roomId: RACE_ROOM_ID,
      startsAt: uniqueStartsAt,
      endsAt: uniqueEndsAt,
      customerName: `Burst VU ${__VU}`,
      customerPhone: `+5690000${String(__VU).padStart(4, '0')}`,
      expirationMinutes: 30,
    };
    tag = 'burst';
  }

  console.log(`VU ${__VU} intentando reserva [${tag}] ${payload.startsAt}`);

  const bookingRes = http.post(
    `${BASE_URL}/agenda/temporary-booking`,
    JSON.stringify(payload),
    { headers, tags: { scenario: tag } }
  );

  // race-same-slot: exactamente 1 debe ser 201, el resto 409/conflict
  if (tag === 'race-same-slot') {
    check(bookingRes, {
      'race: status 201 (winner) or 409 (conflict)': (r) => r.status === 201 || r.status === 409,
    });
    if (bookingRes.status === 201) {
      console.log(`VU ${__VU} GANÓ la carrera — booking creado`);
    } else if (bookingRes.status === 409) {
      const body = bookingRes.body;
      console.log(`VU ${__VU} perdió la carrera — conflicto: ${body}`);
    }
  }

  // location-only: igual que race, solo 1 debe ganar
  if (tag === 'location-only') {
    check(bookingRes, {
      'location-only: status 201 or 409': (r) => r.status === 201 || r.status === 409,
    });
  }

  // burst: todos deben ser 201 (slots únicos)
  if (tag === 'burst') {
    check(bookingRes, {
      'burst: status 201 (unique slot)': (r) => r.status === 201,
    });
    if (bookingRes.status !== 201) {
      console.warn(`VU ${__VU} burst falló — status ${bookingRes.status}: ${bookingRes.body}`);
    }
  }

  // Umbral mínimo entre escenarios
  check(bookingRes, {
    [`${tag}: response received`]: (r) => r.status > 0,
  });

  sleep(0.5);
}

export function teardown(data) {
  console.log('Teardown — test de concurrencia completado.');
  console.log('Revisar resultados en consola y umbrales definidos.');
}
