import { createFileRoute } from '@tanstack/react-router';
import { VotingSessionPage } from '../../../features/voting/voting-session-page';

export const Route = createFileRoute('/_protected/vote/session')({
  component: VotingSessionPage,
});
