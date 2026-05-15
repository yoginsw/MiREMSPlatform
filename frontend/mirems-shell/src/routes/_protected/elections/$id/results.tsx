import { createFileRoute, useParams } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/elections/$id/results')({
  component: ElectionResultsRoutePage,
});

export function ElectionResultsRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <article className="card" aria-labelledby="election-results-title">
      <h3 id="election-results-title">개표 결과</h3>
      <p>선거 {id ?? ''}의 인증된 결과와 보고서 해시를 확인합니다.</p>
    </article>
  );
}
