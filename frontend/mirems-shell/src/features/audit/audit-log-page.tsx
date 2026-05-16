import React from 'react';
import { Alert, Badge, Button, Card, Input } from '@mirems/ui-core';
import type { AuditLogEntry, AuditLogPageResponse } from '@mirems/api-client';
import { useAuth } from '../../auth/useAuth';
import { searchAuditEvents, type AuditSearchParams } from './audit-api';

type AuditFilters = {
  search: string;
  aggregateId: string;
  aggregateType: string;
  actorId: string;
  fromDate: string;
  toDate: string;
};

const DEFAULT_FILTERS: AuditFilters = {
  search: '',
  aggregateId: '',
  aggregateType: '',
  actorId: '',
  fromDate: '',
  toDate: '',
};

export function AuditLogPage() {
  const auth = useAuth();
  const accessToken = auth.user?.access_token;
  const canView = auth.hasRole('AUDITOR') || auth.hasRole('SYSTEM_ADMIN');
  const [filters, setFilters] = React.useState<AuditFilters>(DEFAULT_FILTERS);
  const [submittedFilters, setSubmittedFilters] = React.useState<AuditFilters>(DEFAULT_FILTERS);
  const [page, setPage] = React.useState<AuditLogPageResponse | null>(null);
  const [error, setError] = React.useState<string | null>(null);
  const [exportMessage, setExportMessage] = React.useState<string | null>(null);

  const loadAuditEvents = React.useCallback(async (nextFilters: AuditFilters) => {
    try {
      const params: AuditSearchParams = {
        aggregateId: nextFilters.aggregateId,
        aggregateType: nextFilters.aggregateType,
        from: toStartInstant(nextFilters.fromDate),
        to: toEndInstant(nextFilters.toDate),
        page: 0,
        size: 20,
      };
      const response = await searchAuditEvents(params, accessToken);
      setPage(response);
      setError(null);
    } catch {
      setError('감사 로그를 불러오지 못했습니다.');
    }
  }, [accessToken]);

  React.useEffect(() => {
    if (!canView) {
      return;
    }
    void loadAuditEvents(DEFAULT_FILTERS);
  }, [canView, loadAuditEvents]);

  if (!canView) {
    return <Alert title="접근 제한" variant="warning">감사 로그는 AUDITOR 또는 SYSTEM_ADMIN 권한이 필요합니다.</Alert>;
  }

  const filteredEvents = filterEvents(page?.content ?? [], {
    ...submittedFilters,
    search: filters.search,
    actorId: filters.actorId,
  });

  function updateFilter<K extends keyof AuditFilters>(key: K, value: AuditFilters[K]) {
    setFilters((current) => ({ ...current, [key]: value }));
  }

  function applyFilters(event: React.FormEvent<HTMLFormElement>) {
    event.preventDefault();
    setSubmittedFilters(filters);
    setExportMessage(null);
    void loadAuditEvents(filters);
  }

  function resetFilters() {
    setFilters(DEFAULT_FILTERS);
    setSubmittedFilters(DEFAULT_FILTERS);
    setExportMessage(null);
    void loadAuditEvents(DEFAULT_FILTERS);
  }

  function exportCsv() {
    const csv = toAuditCsv(filteredEvents);
    const blob = new Blob([csv], { type: 'text/csv;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const anchor = document.createElement('a');
    anchor.href = url;
    anchor.download = `audit-log-${new Date().toISOString().slice(0, 10)}.csv`;
    document.body.append(anchor);
    anchor.click();
    anchor.remove();
    URL.revokeObjectURL(url);
    setExportMessage('CSV 파일이 준비되었습니다.');
  }

  return (
    <section className="audit-log-page" aria-labelledby="audit-log-title">
      <header className="page-header-row">
        <div>
          <p className="breadcrumb">감사 로그</p>
          <div className="title-row">
            <h2 id="audit-log-title">감사 로그 조회</h2>
            <Badge variant="neutral">Append-only</Badge>
          </div>
          <p>append-only 감사 이벤트를 검색하고 필터링하며, 현재 조회 결과를 CSV로 내보냅니다.</p>
        </div>
        <div className="wizard-actions">
          <Button type="button" onClick={() => void loadAuditEvents(submittedFilters)}>새로고침</Button>
          <Button type="button" variant="primary" onClick={exportCsv} disabled={filteredEvents.length === 0}>CSV 내보내기</Button>
        </div>
      </header>

      {error ? <Alert title="처리 오류" variant="danger">{error}</Alert> : null}
      {exportMessage ? <Alert title="내보내기" variant="success">{exportMessage}</Alert> : null}

      <Card title="검색 및 필터">
        <form className="form-grid" onSubmit={applyFilters}>
          <Input label="검색어" value={filters.search} onChange={(event) => updateFilter('search', event.target.value)} placeholder="이벤트, 집계 ID, payload 검색" />
          <Input label="집계 유형" value={filters.aggregateType} onChange={(event) => updateFilter('aggregateType', event.target.value)} placeholder="예: VOTING_SESSION" />
          <Input label="집계 ID" value={filters.aggregateId} onChange={(event) => updateFilter('aggregateId', event.target.value)} placeholder="UUID" />
          <Input label="행위자" value={filters.actorId} onChange={(event) => updateFilter('actorId', event.target.value)} placeholder="actorId" />
          <Input label="시작일" type="date" value={filters.fromDate} onChange={(event) => updateFilter('fromDate', event.target.value)} />
          <Input label="종료일" type="date" value={filters.toDate} onChange={(event) => updateFilter('toDate', event.target.value)} />
          <div className="wizard-actions form-actions-span">
            <Button type="submit" variant="primary">필터 적용</Button>
            <Button type="button" onClick={resetFilters}>초기화</Button>
          </div>
        </form>
      </Card>

      <Card title="감사 이벤트">
        <div className="table-summary-row">
          <strong>총 {filteredEvents.length.toLocaleString('ko-KR')}건</strong>
          {page ? <span>서버 전체 {page.totalElements.toLocaleString('ko-KR')}건 / {page.totalPages.toLocaleString('ko-KR')}페이지</span> : null}
        </div>
        {filteredEvents.length === 0 ? <p>조건에 맞는 감사 이벤트가 없습니다.</p> : <AuditTable events={filteredEvents} />}
      </Card>
    </section>
  );
}

function AuditTable({ events }: { events: AuditLogEntry[] }) {
  return (
    <div className="table-scroll" role="region" aria-label="감사 이벤트 표">
      <table className="data-table audit-table">
        <thead>
          <tr>
            <th scope="col">발생 시각</th>
            <th scope="col">이벤트</th>
            <th scope="col">집계 유형</th>
            <th scope="col">집계 ID</th>
            <th scope="col">행위자</th>
            <th scope="col">출처 IP</th>
            <th scope="col">Payload</th>
          </tr>
        </thead>
        <tbody>
          {events.map((entry) => (
            <tr key={entry.id}>
              <td>{formatDateTime(entry.occurredAt)}</td>
              <td>{entry.eventType}</td>
              <td>{entry.aggregateType}</td>
              <td><code>{entry.aggregateId}</code></td>
              <td>{entry.actorId}</td>
              <td>{entry.sourceIp ?? '-'}</td>
              <td><code>{safePayload(entry.payload)}</code></td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function filterEvents(events: AuditLogEntry[], filters: AuditFilters): AuditLogEntry[] {
  const search = filters.search.trim().toLowerCase();
  const actor = filters.actorId.trim().toLowerCase();
  return events.filter((event) => {
    const matchesActor = actor.length === 0 || event.actorId.toLowerCase().includes(actor);
    const haystack = [
      event.eventType,
      event.aggregateType,
      event.aggregateId,
      event.actorId,
      event.sourceIp ?? '',
      safePayload(event.payload),
    ].join(' ').toLowerCase();
    const matchesSearch = search.length === 0 || haystack.includes(search);
    return matchesActor && matchesSearch;
  });
}

function toAuditCsv(events: AuditLogEntry[]): string {
  const rows = [
    ['occurredAt', 'eventType', 'aggregateType', 'aggregateId', 'actorId', 'sourceIp', 'payload'],
    ...events.map((event) => [
      event.occurredAt,
      event.eventType,
      event.aggregateType,
      event.aggregateId,
      event.actorId,
      event.sourceIp ?? '',
      safePayload(event.payload),
    ]),
  ];
  return rows.map((row) => row.map(escapeCsvCell).join(',')).join('\r\n');
}

function escapeCsvCell(value: string): string {
  const neutralizedValue = /^[=+\-@\t\r\n]/.test(value) ? `'${value}` : value;
  if (/[",\r\n]/.test(neutralizedValue)) {
    return `"${neutralizedValue.replace(/"/g, '""')}"`;
  }
  return neutralizedValue;
}

function safePayload(payload: AuditLogEntry['payload']): string {
  try {
    return JSON.stringify(payload);
  } catch {
    return '[unserializable payload]';
  }
}

function toStartInstant(date: string): string | undefined {
  return date ? new Date(`${date}T00:00:00.000Z`).toISOString() : undefined;
}

function toEndInstant(date: string): string | undefined {
  return date ? new Date(`${date}T23:59:59.999Z`).toISOString() : undefined;
}

function formatDateTime(value: string): string {
  return new Date(value).toLocaleString('ko-KR');
}
