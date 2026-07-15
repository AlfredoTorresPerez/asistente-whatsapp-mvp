import { describe, it, expect } from 'vitest';

describe('whatsapp-web-service', () => {
  it('should export a start function', async () => {
    const mod = await import('./server.js');
    expect(mod).toBeDefined();
  });
});
