import { describe, expect, it, vi } from 'vitest';
import { authUpdatedEventName } from './auth-events';
import { completeCallbackNavigation } from './callback-navigation';

function installWindowStub() {
  const listeners = new Map<string, Array<(event: Event) => void>>();
  const replaceState = vi.fn();
  vi.stubGlobal('window', {
    location: { origin: 'https://ops.example' },
    history: { replaceState },
    addEventListener: vi.fn((name: string, listener: (event: Event) => void) => {
      listeners.set(name, [...(listeners.get(name) ?? []), listener]);
    }),
    removeEventListener: vi.fn(),
    dispatchEvent: vi.fn((event: Event) => {
      listeners.get(event.type)?.forEach((listener) => listener(event));
      return true;
    }),
  });

  return { listeners, replaceState };
}

describe('completeCallbackNavigation', () => {
  it('uses same-document navigation so in-memory OIDC user storage survives callback completion', () => {
    const { replaceState } = installWindowStub();
    const listener = vi.fn();
    window.addEventListener(authUpdatedEventName, listener);

    const finalPath = completeCallbackNavigation('/miremsplatform/audit');

    expect(finalPath).toBe('/miremsplatform/audit');
    expect(replaceState).toHaveBeenCalledWith(null, '', '/miremsplatform/audit');
    expect(listener).toHaveBeenCalledTimes(1);

    vi.unstubAllGlobals();
  });

  it('sanitizes callback return paths before updating browser state', () => {
    const { replaceState } = installWindowStub();

    expect(completeCallbackNavigation('https://evil.example/phish')).toBe('/miremsplatform/elections');
    expect(replaceState).toHaveBeenCalledWith(null, '', '/miremsplatform/elections');

    vi.unstubAllGlobals();
  });
});
