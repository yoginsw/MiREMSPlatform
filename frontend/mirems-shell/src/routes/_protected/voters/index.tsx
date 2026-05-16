import { createFileRoute } from '@tanstack/react-router';
import { VoterRollAdminPage } from '../../../features/voters/voter-pages';

export const Route = createFileRoute('/_protected/voters/')({
  component: VoterRollAdminPage,
});
