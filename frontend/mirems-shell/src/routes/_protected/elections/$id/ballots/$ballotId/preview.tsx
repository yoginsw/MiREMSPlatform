import { createFileRoute, useParams } from '@tanstack/react-router';
import { BallotPreviewPage } from '../../../../../../features/ballots/ballot-pages';

export const Route = createFileRoute('/_protected/elections/$id/ballots/$ballotId/preview')({
  component: BallotPreviewRoutePage,
});

export function BallotPreviewRoutePage() {
  const { id, ballotId } = useParams({ strict: false });
  return <BallotPreviewPage electionId={id ?? ''} ballotId={ballotId ?? ''} />;
}
