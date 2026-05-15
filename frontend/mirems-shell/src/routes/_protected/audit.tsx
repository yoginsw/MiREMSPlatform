import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/audit')({
  component: AuditRoutePage,
});

export function AuditRoutePage() {
  return (
    <section className="page-header" aria-labelledby="audit-title">
      <div>
        <p className="breadcrumb">감사 로그</p>
        <div className="title-row">
          <h2 id="audit-title">감사 로그 조회</h2>
          <span className="badge badge--certified">AUDITOR</span>
        </div>
        <p>append-only 감사 이벤트를 검색하고 검증하는 보호 화면입니다.</p>
      </div>
    </section>
  );
}
