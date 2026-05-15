import { describe, expect, it } from 'vitest';
import { swaggerUiHref } from './api-docs-link';

describe('Swagger UI link', () => {
  it('points to the backend Swagger UI under the MiREMS Platform base path', () => {
    expect(swaggerUiHref).toBe('/miremsplatform/swagger-ui/index.html');
  });
});
