import { describe, expect, it, vi } from 'vitest';
import { completeSilentRenew } from './auth-callback';

describe('OIDC callback helpers', () => {
  it('completes silent renew callbacks through the user manager', async () => {
    const userManager = {
      signinSilentCallback: vi.fn().mockResolvedValue(undefined),
    };

    await expect(completeSilentRenew(userManager)).resolves.toBeUndefined();

    expect(userManager.signinSilentCallback).toHaveBeenCalledOnce();
  });
});
