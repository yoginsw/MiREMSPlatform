import { createFileRoute, useParams } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/elections/$id/ballots')({
  component: ElectionBallotsRoutePage,
});

export function ElectionBallotsRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <article className="card" aria-labelledby="election-ballots-title">
      <h3 id="election-ballots-title">투표용지 관리</h3>
      <p>선거 {id ?? ''}의 Ballot 및 BallotStyle 구성을 관리합니다.</p>
    </article>
  );
}
