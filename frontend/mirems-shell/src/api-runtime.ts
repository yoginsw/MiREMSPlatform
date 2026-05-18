const PLATFORM_API_PATH = '/miremsplatform';
const DEFAULT_LOCAL_API_ORIGIN = 'http://localhost:8080';

export interface ApiRuntimeEnv {
  DEV?: boolean;
  MODE?: string;
  VITE_API_BASE_URL?: string;
  VITE_API_ORIGIN?: string;
}

export function resolveApiBasePath(env: ApiRuntimeEnv = import.meta.env): string {
  const explicitBaseUrl = trimTrailingSlash(env.VITE_API_BASE_URL);
  if (explicitBaseUrl) {
    return explicitBaseUrl;
  }

  if (env.DEV && env.MODE !== 'test') {
    const apiOrigin = trimTrailingSlash(env.VITE_API_ORIGIN) ?? DEFAULT_LOCAL_API_ORIGIN;
    return `${apiOrigin}${PLATFORM_API_PATH}`;
  }

  return PLATFORM_API_PATH;
}

export function resolveApiUrl(path: string, env: ApiRuntimeEnv = import.meta.env): string {
  const basePath = resolveApiBasePath(env);
  return `${basePath}/${path.replace(/^\/+/, '')}`;
}

function trimTrailingSlash(value: string | undefined): string | undefined {
  const trimmed = value?.trim().replace(/\/+$/, '');
  return trimmed ? trimmed : undefined;
}
