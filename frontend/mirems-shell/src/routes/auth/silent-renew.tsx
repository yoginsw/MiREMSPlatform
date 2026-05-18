import { createFileRoute } from '@tanstack/react-router';
import { SilentRenewPage } from '../../auth/SilentRenewPage';

export const Route = createFileRoute('/auth/silent-renew')({
  component: SilentRenewPage,
});
