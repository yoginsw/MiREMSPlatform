import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/elections/')({
  component: ElectionListRoutePage,
});

export function ElectionListRoutePage() {
  return (
    <section className="page-header" aria-labelledby="election-list-title">
      <div>
        <p className="breadcrumb">선거 관리</p>
        <div className="title-row">
          <h2 id="election-list-title">선거 목록</h2>
          <span className="badge badge--pending">ROUTED</span>
        </div>
        <p>등록된 선거를 조회하고 상태, 유형, 관할 기준으로 관리하는 화면입니다.</p>
      </div>
    </section>
  );
}
