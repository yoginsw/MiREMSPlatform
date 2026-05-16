import { createFileRoute, Outlet, useParams } from '@tanstack/react-router';
import { BallotStyleManagementPage } from '../../../../features/ballots/ballot-pages';

export const Route = createFileRoute('/_protected/elections/$id/ballots')({
  component: ElectionBallotsRoutePage,
});

export function ElectionBallotsRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <>
      <BallotStyleManagementPage electionId={id ?? ''} />
      <Outlet />
    </>
  );
}
