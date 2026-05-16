import { http, HttpResponse } from 'msw';
import { describe, expect, it } from 'vitest';
import { server } from '../test/msw-server';
import { createFrontendErrorReport, reportFrontendError } from './error-reporting';

describe('frontend error reporting', () => {
  it('posts sanitized frontend render errors to the AuditLog reporting endpoint with Authorization', async () => {
    let capturedBody: unknown;
    let capturedAuth: string | null = null;

    server.use(
      http.post('/miremsplatform/audit/frontend-errors', async ({ request }) => {
        capturedAuth = request.headers.get('authorization');
        capturedBody = await request.json();
        return HttpResponse.json({ accepted: true }, { status: 202 });
      }),
    );

    await reportFrontendError(
      createFrontendErrorReport({
        error: new Error('Sensitive runtime trace must not be exposed'),
        componentStack: 'at BrokenWidget (/src/secret-token.tsx:10:5)',
        path: '/miremsplatform/elections',
      }),
      'test-access-token',
    );

    expect(capturedAuth).toBe('Bearer test-access-token');
    expect(capturedBody).toMatchObject({
      eventType: 'FRONTEND_RENDER_ERROR',
      aggregateType: 'FrontendRuntime',
      path: '/miremsplatform/elections',
      errorName: 'Error',
      message: 'Sensitive runtime trace must not be exposed',
    });
    expect(JSON.stringify(capturedBody)).not.toContain('secret-token');
    expect(JSON.stringify(capturedBody)).not.toContain('stack');
  });

  it('does not call the reporting endpoint when there is no bearer token', async () => {
    let calls = 0;
    server.use(
      http.post('/miremsplatform/audit/frontend-errors', () => {
        calls += 1;
        return HttpResponse.json({ accepted: true }, { status: 202 });
      }),
    );

    await reportFrontendError(createFrontendErrorReport({ error: new Error('boom'), path: '/miremsplatform/admin' }));

    expect(calls).toBe(0);
  });

  it('redacts sensitive query, hash, token, email, and file-path fragments from report payloads', () => {
    const report = createFrontendErrorReport({
      error: new Error('Bearer abc.def.ghi failed for admin@example.test at D:/workspace/MiREMSPlatform/frontend/src/secret.tsx with password=hunter2'),
      path: '/miremsplatform/admin?access_token=abc.def.ghi&email=admin@example.test#token=abc.def.ghi',
    });

    const serialized = JSON.stringify(report);
    expect(report.path).toBe('/miremsplatform/admin');
    expect(serialized).not.toContain('abc.def.ghi');
    expect(serialized).not.toContain('admin@example.test');
    expect(serialized).not.toContain('hunter2');
    expect(serialized).not.toContain('secret.tsx');
    expect(report.message).toContain('Bearer [redacted]');
    expect(report.message).toContain('password=[redacted]');
    expect(report.message).toContain('[redacted-email]');
    expect(report.message).toContain('[redacted-path]');
  });
});
