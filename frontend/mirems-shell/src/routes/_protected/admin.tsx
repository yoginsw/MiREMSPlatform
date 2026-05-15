import { createFileRoute } from '@tanstack/react-router';

export const Route = createFileRoute('/_protected/admin')({
  component: AdminRoutePage,
});

export function AdminRoutePage() {
  return (
    <section className="page-header" aria-labelledby="admin-title">
      <div>
        <p className="breadcrumb">시스템 관리</p>
        <div className="title-row">
          <h2 id="admin-title">관리자 콘솔</h2>
          <span className="badge badge--pending">SYSTEM_ADMIN</span>
        </div>
        <p>시스템 상태, BPMN 프로세스, 확장팩 상태를 관리하는 보호 화면입니다.</p>
      </div>
    </section>
  );
}
