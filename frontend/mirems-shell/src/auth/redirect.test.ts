import { describe, expect, it } from 'vitest';
import { sanitizeReturnUrl } from './redirect';

describe('sanitizeReturnUrl', () => {
  const origin = 'https://ops.example';

  it('allows platform-relative return paths', () => {
    expect(sanitizeReturnUrl('/miremsplatform/audit?tab=security#latest', origin)).toBe('/miremsplatform/audit?tab=security#latest');
  });

  it('rejects external absolute URLs', () => {
    expect(sanitizeReturnUrl('https://evil.example/phish', origin)).toBe('/miremsplatform');
  });

  it('rejects protocol-relative URLs and javascript URLs', () => {
    expect(sanitizeReturnUrl('//evil.example/phish', origin)).toBe('/miremsplatform');
    expect(sanitizeReturnUrl('javascript:alert(1)', origin)).toBe('/miremsplatform');
  });

  it('rejects same-origin paths outside the platform base path', () => {
    expect(sanitizeReturnUrl('/admin', origin)).toBe('/miremsplatform');
  });
});
