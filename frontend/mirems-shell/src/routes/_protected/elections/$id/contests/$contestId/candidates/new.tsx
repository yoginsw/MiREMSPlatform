import { createFileRoute, useParams } from '@tanstack/react-router';
import { CandidateRegistrationForm } from '../../../../../../../features/candidates/candidate-pages';

export const Route = createFileRoute('/_protected/elections/$id/contests/$contestId/candidates/new')({
  component: CandidateRegistrationRoutePage,
});

export function CandidateRegistrationRoutePage() {
  const { id, contestId } = useParams({ strict: false });
  return <CandidateRegistrationForm electionId={id ?? ''} contestId={contestId ?? ''} />;
}
