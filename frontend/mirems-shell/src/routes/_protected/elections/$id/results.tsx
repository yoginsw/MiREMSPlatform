import { Outlet, createFileRoute, useParams } from '@tanstack/react-router';
import { ResultsDashboardPage } from '../../../../features/results/results-pages';

export const Route = createFileRoute('/_protected/elections/$id/results')({
  component: ElectionResultsRoutePage,
});

export function ElectionResultsRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <>
      <ResultsDashboardPage electionId={id ?? ''} />
      <Outlet />
    </>
  );
}
