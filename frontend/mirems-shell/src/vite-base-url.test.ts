import { describe, expect, it } from 'vitest';
import viteConfig from '../vite.config';

describe('Vite base URL', () => {
  it('serves the shell below the MiREMS Platform base path', () => {
    expect(viteConfig.base).toBe('/miremsplatform/');
  });
});
