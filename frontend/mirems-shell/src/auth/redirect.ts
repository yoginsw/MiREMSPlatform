import { PLATFORM_BASE_PATH, platformHref } from '../navigation';

export const defaultAuthenticatedReturnUrl = platformHref('/elections');
const fallbackReturnUrl = defaultAuthenticatedReturnUrl;

export function sanitizeReturnUrl(value: string | null | undefined, origin = window.location.origin): string {
  if (!value) {
    return fallbackReturnUrl;
  }

  try {
    const url = new URL(value, origin);
    if (url.origin !== origin) {
      return fallbackReturnUrl;
    }

    const normalizedBase = PLATFORM_BASE_PATH.endsWith('/') ? PLATFORM_BASE_PATH.slice(0, -1) : PLATFORM_BASE_PATH;
    const isPlatformRoot = url.pathname === normalizedBase;
    const isPlatformChild = url.pathname.startsWith(`${normalizedBase}/`);

    if (!isPlatformRoot && !isPlatformChild) {
      return fallbackReturnUrl;
    }

    return `${url.pathname}${url.search}${url.hash}`;
  } catch {
    return fallbackReturnUrl;
  }
}
