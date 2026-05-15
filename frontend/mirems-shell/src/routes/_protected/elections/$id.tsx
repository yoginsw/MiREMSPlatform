import { createFileRoute, Outlet, useParams } from '@tanstack/react-router';
import { ElectionDetailPage } from '../../../features/elections/election-pages';

export const Route = createFileRoute('/_protected/elections/$id')({
  component: ElectionDetailRoutePage,
});

export function ElectionDetailRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <>
      <ElectionDetailPage electionId={id ?? ''} />
      <Outlet />
    </>
  );
}
