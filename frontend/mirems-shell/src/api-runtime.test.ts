import { describe, expect, it } from 'vitest';
import { resolveApiBasePath, resolveApiUrl } from './api-runtime';

describe('API runtime URL helpers', () => {
  it('uses the backend origin during local Vite development instead of serving API calls from the SPA dev server', () => {
    expect(resolveApiBasePath({ DEV: true, MODE: 'development' })).toBe('http://localhost:8080/miremsplatform');
  });

  it('keeps relative platform URLs for tests and production unless explicitly configured', () => {
    expect(resolveApiBasePath({ DEV: false, MODE: 'production' })).toBe('/miremsplatform');
    expect(resolveApiBasePath({ DEV: true, MODE: 'test' })).toBe('/miremsplatform');
  });

  it('honors an explicit API base URL and joins endpoint paths safely', () => {
    const env = { DEV: true, MODE: 'development', VITE_API_BASE_URL: 'https://api.example/miremsplatform/' };

    expect(resolveApiBasePath(env)).toBe('https://api.example/miremsplatform');
    expect(resolveApiUrl('/elections', env)).toBe('https://api.example/miremsplatform/elections');
  });
});
