import { describe, expect, it } from 'vitest';
import { MemoryStorage } from './oidc-storage';

describe('MemoryStorage', () => {
  it('keeps OIDC user tokens in process memory only', () => {
    const storage = new MemoryStorage();

    storage.setItem('user', 'token-payload');

    expect(storage.getItem('user')).toBe('token-payload');
    expect(storage.length).toBe(1);
    storage.removeItem('user');
    expect(storage.getItem('user')).toBeNull();
  });
});
