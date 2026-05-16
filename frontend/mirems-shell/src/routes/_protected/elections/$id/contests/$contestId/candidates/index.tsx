import { createFileRoute, useParams } from '@tanstack/react-router';
import { CandidateListPage } from '../../../../../../../features/candidates/candidate-pages';

export const Route = createFileRoute('/_protected/elections/$id/contests/$contestId/candidates/')({
  component: CandidateListRoutePage,
});

export function CandidateListRoutePage() {
  const { id, contestId } = useParams({ strict: false });
  return <CandidateListPage electionId={id ?? ''} contestId={contestId ?? ''} />;
}
