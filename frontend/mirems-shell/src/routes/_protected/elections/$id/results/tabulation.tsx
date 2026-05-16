import { createFileRoute, useParams } from '@tanstack/react-router';
import { TabulationProgressPage } from '../../../../../features/results/results-pages';

export const Route = createFileRoute('/_protected/elections/$id/results/tabulation')({
  component: TabulationProgressRoutePage,
});

export function TabulationProgressRoutePage() {
  const { id } = useParams({ strict: false });
  return <TabulationProgressPage electionId={id ?? ''} />;
}
