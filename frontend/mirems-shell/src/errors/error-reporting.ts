import type { ErrorInfo } from 'react';

export interface FrontendErrorReportInput {
  error: unknown;
  componentStack?: string;
  path?: string;
}

export interface FrontendErrorReport {
  eventType: 'FRONTEND_RENDER_ERROR';
  aggregateType: 'FrontendRuntime';
  path: string;
  errorName: string;
  message: string;
  componentTrace?: string;
  occurredAt: string;
}

const MAX_FIELD_LENGTH = 512;

export function createFrontendErrorReport({ error, componentStack, path = currentPath() }: FrontendErrorReportInput): FrontendErrorReport {
  const normalized = normalizeError(error);
  return {
    eventType: 'FRONTEND_RENDER_ERROR',
    aggregateType: 'FrontendRuntime',
    path: sanitizePath(path),
    errorName: sanitizeText(normalized.name || 'Error'),
    message: sanitizeMessage(normalized.message || 'Unknown frontend error'),
    componentTrace: componentStack ? sanitizeComponentTrace(componentStack) : undefined,
    occurredAt: new Date().toISOString(),
  };
}

export function createFrontendErrorReportFromInfo(error: unknown, errorInfo?: ErrorInfo, path?: string): FrontendErrorReport {
  return createFrontendErrorReport({ error, componentStack: errorInfo?.componentStack ?? undefined, path });
}

export async function reportFrontendError(report: FrontendErrorReport, accessToken?: string): Promise<void> {
  if (!accessToken) {
    return;
  }

  try {
    const response = await fetch('/miremsplatform/audit/frontend-errors', {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${accessToken}`,
        'Content-Type': 'application/json',
      },
      body: JSON.stringify(report),
    });

    if (!response.ok) {
      // Error reporting must never break user recovery flows.
    }
  } catch {
    // Error reporting must never break user recovery flows.
  }
}

function normalizeError(error: unknown): { name: string; message: string } {
  if (error instanceof Error) {
    return { name: error.name, message: error.message };
  }
  return { name: 'Error', message: String(error) };
}

function sanitizeComponentTrace(componentStack: string): string {
  return sanitizeText(
    componentStack
      .split('\n')
      .map((line) => line.replace(/\([^)]*\)/g, '(redacted)'))
      .join('\n'),
  );
}

function sanitizePath(path: string): string {
  const pathWithoutQueryOrHash = path.split(/[?#]/, 1)[0] || '/miremsplatform/';
  return sanitizeText(redactSensitiveFragments(pathWithoutQueryOrHash));
}

function sanitizeMessage(message: string): string {
  return sanitizeText(redactSensitiveFragments(message));
}

function redactSensitiveFragments(value: string): string {
  return value
    .replace(/bearer\s+[a-z0-9._~+/=-]+/gi, 'Bearer [redacted]')
    .replace(/\b(access_token|api_key|password|secret|token)=([^\s&]+)/gi, '$1=[redacted]')
    .replace(/\b[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\.[A-Za-z0-9_-]+\b/g, '[redacted-jwt]')
    .replace(/[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}/g, '[redacted-email]')
    .replace(/([A-Za-z]:)?[\\/][^\s)]+\.(tsx?|jsx?|java|kt|sql|ya?ml|json)/g, '[redacted-path]');
}

function sanitizeText(value: string): string {
  return [...value]
    .map((char) => {
      const codePoint = char.codePointAt(0) ?? 0;
      return codePoint < 32 || codePoint === 127 ? ' ' : char;
    })
    .join('')
    .slice(0, MAX_FIELD_LENGTH);
}

function currentPath(): string {
  if (typeof window === 'undefined') {
    return '/miremsplatform/';
  }
  return `${window.location.pathname}${window.location.search}${window.location.hash}`;
}
