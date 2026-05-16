import { createFileRoute, useParams } from '@tanstack/react-router';
import { CandidateReviewPage } from '../../../../../../../features/candidates/candidate-pages';

export const Route = createFileRoute('/_protected/elections/$id/contests/$contestId/candidates/review')({
  component: CandidateReviewRoutePage,
});

export function CandidateReviewRoutePage() {
  const { id, contestId } = useParams({ strict: false });
  return <CandidateReviewPage electionId={id ?? ''} contestId={contestId ?? ''} />;
}
