import { createFileRoute } from '@tanstack/react-router';
import { VoterEligibilityCheckPage } from '../../../features/voters/voter-pages';

export const Route = createFileRoute('/_protected/voters/eligibility')({
  component: VoterEligibilityCheckPage,
});
