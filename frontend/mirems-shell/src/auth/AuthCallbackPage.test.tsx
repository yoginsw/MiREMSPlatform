import React from 'react';
import { render, waitFor } from '@testing-library/react';
import { describe, expect, it, vi } from 'vitest';
import { AuthCallbackPage } from './AuthCallbackPage';
import { completeSigninRedirect } from './auth-callback';
import { completeCallbackNavigation } from './callback-navigation';

vi.mock('./auth-callback', () => ({
  completeSigninRedirect: vi.fn(),
}));

vi.mock('./callback-navigation', () => ({
  completeCallbackNavigation: vi.fn(),
}));

describe('AuthCallbackPage', () => {
  it('handles the OIDC authorization code only once under React StrictMode', async () => {
    vi.mocked(completeSigninRedirect).mockResolvedValue('/miremsplatform/audit');

    render(
      <React.StrictMode>
        <AuthCallbackPage />
      </React.StrictMode>,
    );

    await waitFor(() => {
      expect(completeCallbackNavigation).toHaveBeenCalledWith('/miremsplatform/audit');
    });

    expect(completeSigninRedirect).toHaveBeenCalledTimes(1);
    expect(completeCallbackNavigation).toHaveBeenCalledTimes(1);
  });
});
