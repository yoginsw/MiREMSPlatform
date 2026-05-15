import { createFileRoute, Outlet, useParams } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/elections/$id')({
  component: ElectionDetailRoutePage,
});

export function ElectionDetailRoutePage() {
  const { id } = useParams({ strict: false });
  return (
    <section className="page-header" aria-labelledby="election-detail-title">
      <div>
        <p className="breadcrumb">선거 관리 / {id ?? '선거 상세'}</p>
        <div className="title-row">
          <h2 id="election-detail-title">선거 상세</h2>
          <span className="badge badge--active">ACTIVE</span>
        </div>
        <p>선거 기본 정보, 경합, 투표용지, 결과 상태를 확인합니다.</p>
      </div>
      <Outlet />
    </section>
  );
}
