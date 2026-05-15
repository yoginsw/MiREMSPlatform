export const authUpdatedEventName = 'mirems:auth-updated';

export function notifyAuthUpdated(): void {
  window.dispatchEvent(new Event(authUpdatedEventName));
}
