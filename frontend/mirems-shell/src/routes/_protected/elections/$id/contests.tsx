import { createFileRoute, useParams } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/elections/$id/contests')({
  component: ElectionContestsRoutePage,
});

export function ElectionContestsRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <article className="card" aria-labelledby="election-contests-title">
      <h3 id="election-contests-title">경합 관리</h3>
      <p>선거 {id ?? ''}의 Contest 목록과 후보자 구성을 관리합니다.</p>
    </article>
  );
}
